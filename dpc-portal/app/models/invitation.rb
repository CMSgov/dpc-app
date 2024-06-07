# frozen_string_literal: true

# Record of invitation, with possible verification code
class Invitation < ApplicationRecord
  attr_reader :phone_raw

  validates :invited_by, :invited_given_name, :invited_family_name, :phone_raw, presence: true, if: :needs_validation?
  validates :invited_email, :invited_email_confirmation, presence: true, if: :new_record?
  validates :invited_email, format: Devise.email_regexp, confirmation: true, if: :new_record?
  validates :invitation_type, presence: true
  validates :invited_phone, format: { with: /\A[0-9]{10}\z/ }, if: :needs_validation?
  validate :cannot_cancel_accepted

  enum invitation_type: %i[credential_delegate authorized_official]
  enum :status, %i[pending accepted expired cancelled], default: :pending

  belongs_to :provider_organization, required: true
  belongs_to :invited_by, class_name: 'User', required: false

  def phone_raw=(nbr)
    @phone_raw = nbr
    self.invited_phone = @phone_raw.tr('^0-9', '')
  end

  def show_attributes
    { full_name: "#{invited_given_name} #{invited_family_name}",
      email: invited_email,
      id:,
      verification_code: }.with_indifferent_access
  end

  def expired?
    created_at < 2.days.ago
  end

  def accept!
    update!(invited_given_name: nil, invited_family_name: nil, invited_phone: nil, invited_email: nil,
            status: :accepted)
  end

  def renew
    return unless expired? && authorized_official?

    invitation = Invitation.create(invited_email:,
                                   invited_email_confirmation:,
                                   provider_organization:,
                                   invitation_type:)

    InvitationMailer.with(invitation:, given_name: invited_given_name,
                          family_name: invited_family_name).invite_ao.deliver_now
    invitation
  end

  def match_user?(user_info)
    if credential_delegate?
      cd_match?(user_info)
    elsif authorized_official?
      ao_match?(user_info)
    end
  end

  def unacceptable_reason
    if expired? && authorized_official?
      'ao_expired'
    elsif expired? || accepted? || cancelled?
      'invalid'
    end
  end

  private

  def cd_match?(user_info)
    return false unless invited_given_name.downcase == user_info['given_name'].downcase &&
                        invited_family_name.downcase == user_info['family_name'].downcase

    return false unless phone_match(user_info)

    user_info['all_emails'].any? { |email| invited_email.downcase == email.downcase }
  end

  # rubocop:disable Metrics/AbcSize
  # Go ahead and pass if one or the other starts with US country code (1)
  def phone_match(user_info)
    user_phone = user_info['phone'].tr('^0-9', '')
    if user_phone.length == invited_phone.length
      user_phone == invited_phone
    elsif user_phone.length > invited_phone.length && user_phone[0] == '1'
      user_phone[1..] == invited_phone
    elsif user_phone.length < invited_phone.length && invited_phone[0] == '1'
      user_phone == invited_phone[1..]
    end
  end
  # rubocop:enable Metrics/AbcSize

  def ao_match?(user_info)
    service = AoVerificationService.new
    result = service.check_eligibility(provider_organization.npi,
                                       Digest::SHA2.new(256).hexdigest(user_info['social_security_number']))
    raise InvitationError, result[:failure_reason] unless result[:success]

    result[:success]
  end

  def cannot_cancel_accepted
    return unless status_was == 'accepted' && cancelled?

    errors.add(:status, :cancel_accepted, message: 'You may not cancel an accepted invitation.')
  end

  def needs_validation?
    new_record? && credential_delegate?
  end
end

class InvitationError < StandardError; end
