# frozen_string_literal: true

# Placeholder for eventual ActiveRecord for saving this
class CdInvitation
  include ActiveModel::API

  attr_accessor :given_name, :family_name, :phone, :email, :email_confirmation
  attr_reader :phone_raw

  validates :given_name, :family_name, :phone_raw, :email, :email_confirmation, presence: true
  validates :email, format: Devise.email_regexp, confirmation: true
  validates :phone, format: { with: /\A[0-9]{10}\z/ }

  def initialize(**args)
    @given_name = args.fetch(:given_name, '')
    @family_name = args.fetch(:family_name, '')
    @phone_raw = args.fetch(:phone_raw, '')
    @phone = @phone_raw.tr('^0-9', '')
    @email = args.fetch(:email, '')
    @email_confirmation = args.fetch(:email_confirmation, '')
  end

  def phone_raw=(nbr)
    @phone_raw = nbr
    @phone = @phone_raw.tr('^0-9', '')
  end
end
