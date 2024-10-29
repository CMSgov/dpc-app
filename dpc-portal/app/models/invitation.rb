# frozen_string_literal: true

# Record of invitation, with possible verification code
class Invitation < ApplicationRecord
  validates :invited_by, :invited_given_name, :invited_family_name, presence: true, if: :needs_validation?
  validates :invited_email, :invited_email_confirmation, presence: true, if: :new_record?
  validates :invited_email, format: Devise.email_regexp, confirmation: true, if: :new_record?
  validates :invitation_type, presence: true
  validate :cannot_cancel_accepted
  validate :check_if_duplicate, if: :new_record?

  enum invitation_type: %i[credential_delegate authorized_official]
  enum :status, %i[pending accepted expired cancelled renewed], default: :pending

  belongs_to :provider_organization, required: true
  belongs_to :invited_by, class_name: 'User', required: false

  AO_STEPS = ['Sign in or create a Login.gov account', 'Confirm your identity', 'Confirm organization registration',
              'Finished'].freeze
  CD_STEPS = ['Sign in or create a Login.gov account', 'Accept invite', 'Finished'].freeze

  def show_attributes
    { full_name: "#{invited_given_name} #{invited_family_name}",
      email: invited_email,
      id: }.with_indifferent_access
  end

  def invited_by_full_name
    "#{invited_by&.given_name} #{invited_by&.family_name}"
  end

  def expired?
    created_at < 2.days.ago
  end

  def accept!
    update!(invited_given_name: nil, invited_family_name: nil, invited_email: nil,
            status: :accepted)
  end

  def renew
    return unless pending? && expired? && authorized_official?

    invitation = Invitation.create!(invited_email:,
                                    invited_email_confirmation: invited_email,
                                    provider_organization:,
                                    invitation_type:)

    InvitationMailer.with(invitation:, given_name: invited_given_name,
                          family_name: invited_family_name).invite_ao.deliver_now
    update(status: :renewed)
    Rails.logger.info(['Authorized Official renewed expired invitation',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::AoRenewedExpiredInvitation }])
    invitation
  end

  def ao_match?(user_info)
    check_missing_user_info(user_info, 'social_security_number')

    service = AoVerificationService.new
    result = service.check_eligibility(provider_organization.npi,
                                       user_info['social_security_number'].tr('-', ''))
    raise VerificationError, result[:failure_reason] unless result[:success]

    result
  end

  def unacceptable_reason # rubocop:disable Metrics/CyclomaticComplexity,Metrics/PerceivedComplexity
    return 'invalid' if cancelled?
    return 'ao_renewed' if renewed? && authorized_official?
    return 'ao_accepted' if accepted? && authorized_official?
    return 'cd_accepted' if accepted? && credential_delegate?
    return 'ao_expired' if expired? && authorized_official?

    'cd_expired' if expired? && credential_delegate?
  end

  def expires_in
    diff = 48.hours - (Time.now - created_at).round
    hours, seconds = diff.divmod(1.hour)
    minutes = seconds / 1.minute
    [hours, minutes]
  end

  def cd_match?(user_info)
    cd_info_present?(user_info)
    invited_family_name.downcase == user_info['family_name'].downcase
  end

  def email_match?(user_info)
    check_missing_user_info(user_info, 'email')

    user_info['email'].downcase == invited_email.downcase
  end

  def check_if_duplicate
    return unless credential_delegate? && (existing_invite? || existing_credential_delegate?)

    errors.add :base, :duplicate
  end

  def existing_invite?
    return false unless provider_organization

    Invitation.where(provider_organization:, invited_email:, invited_given_name:, invited_family_name:,
                     status: :pending).any?
  end

  def existing_credential_delegate?
    return false unless provider_organization&.cd_org_links

    provider_organization.cd_org_links.any? do |link|
      link.disabled_at.nil? && link.user.email == :invited_email &&
        link.user.given_name == :invited_given_name && link.user.family_name == :invited_family_name
    end
  end

  private

  def cd_info_present?(user_info)
    %w[given_name family_name].each do |key|
      check_missing_user_info(user_info, key)
    end
  end

  def check_missing_user_info(user_info, key)
    return if user_info[key].present?

    Rails.logger.error("User Info Missing: #{key}")
    raise UserInfoServiceError, 'missing_info'
  end

  def cannot_cancel_accepted
    return unless status_was == 'accepted' && cancelled?

    errors.add(:status, :cancel_accepted, message: 'You may not cancel an accepted invitation.')
  end

  def needs_validation?
    new_record? && credential_delegate?
  end
end

class VerificationError < StandardError; end
