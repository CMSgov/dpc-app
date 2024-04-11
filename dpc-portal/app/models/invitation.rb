# frozen_string_literal: true

# Record of invitation, with possible verification code
class Invitation < ApplicationRecord
  attr_reader :phone_raw

  validates :invited_by, :invited_given_name, :invited_family_name, :phone_raw, presence: true, if: :needs_validation?
  validates :invited_email, :invited_email_confirmation, presence: true, if: :new_record?
  validates :invited_email, format: Devise.email_regexp, confirmation: true, if: :new_record?
  validates :invitation_type, presence: true
  validates :invited_phone, format: { with: /\A[0-9]{10}\z/ }, if: :needs_validation?

  enum invitation_type: %i[credential_delegate authorized_official]

  belongs_to :provider_organization, required: true
  belongs_to :invited_by, class_name: 'User', required: false

  def phone_raw=(nbr)
    @phone_raw = nbr
    self.invited_phone = @phone_raw.tr('^0-9', '')
  end

  def show_attributes
    { full_name: "#{invited_given_name} #{invited_family_name}",
      email: invited_email,
      verification_code: }.with_indifferent_access
  end

  def expired?
    created_at < 2.days.ago
  end

  def accepted?
    if invitation_type == 'credential_delegate'
      CdOrgLink.where(invitation: self).exists? 
    elsif invitation_type == 'authorized_official'
      AoOrgLink.where(invitation: self).exists?
    end
  end

  def match_user?(user)
    if invitation_type == 'credential_delegate'
      invited_given_name.downcase == user.given_name.downcase &&
        invited_family_name.downcase == user.family_name.downcase &&
        invited_email.downcase == user.email.downcase
    elsif invitation_type == 'authorized_official'
      invited_email.downcase == user.email.downcase
    end
  end

  private

  def needs_validation?
    new_record? && invitation_type == 'credential_delegate'
  end
end
