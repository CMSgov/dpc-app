# frozen_string_literal: true

# Record of invitation, with possible verification code
class Invitation < ApplicationRecord
  attr_reader :phone_raw

  validates :invited_given_name, :invited_family_name, :phone_raw, :invited_email, :invited_email_confirmation,
            presence: true, if: :new_record?
  validates :invited_email, format: Devise.email_regexp, confirmation: true, if: :new_record?
  validates :invitation_type, presence: true
  validates :invited_phone, format: { with: /\A[0-9]{10}\z/ }, if: :new_record?

  enum invitation_type: %i[credential_delegate]

  belongs_to :provider_organization, required: true
  belongs_to :invited_by, class_name: 'User', required: true

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
    CdOrgLink.where(invitation: self).exists?
  end
end
