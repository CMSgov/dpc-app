# frozen_string_literal: true

# Placeholder for eventual ActiveRecord for saving this
class Invitation < ApplicationRecord
  attr_reader :phone_raw

  validates :invited_given_name, :invited_family_name, :phone_raw, :invited_email, :invited_email_confirmation,
            presence: true
  validates :invited_email, format: Devise.email_regexp, confirmation: true
  validates :invited_phone, format: { with: /\A[0-9]{10}\z/ }
  validates :invitation_type, inclusion: { in: %w[credential_delegate] }

  belongs_to :provider_organization, required: true
  belongs_to :invited_by, class_name: 'User', required: true

  def phone_raw=(nbr)
    @phone_raw = nbr
    self.invited_phone = @phone_raw.tr('^0-9', '')
  end
end
