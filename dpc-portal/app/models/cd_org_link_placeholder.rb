# frozen_string_literal: true

# Placeholder for eventual ActiveRecord for saving this
class CdOrgLinkPlaceholder
  attr_reader :given_name, :family_name, :email, :pending, :verification_code, :activated_at

  def initialize(given_name:, family_name:, email:, pending:, verification_code: nil)
    @given_name = given_name
    @family_name = family_name
    @email = email
    @pending = pending
    @verification_code = verification_code || (Array('A'..'Z') + Array(0..9)).sample(6).join
    @activated_at = 1.day.ago
  end

  def show_attributes
    { full_name: "#{given_name} #{family_name}",
      email:,
      verification_code:,
      activated_at: activated_at.to_s }.with_indifferent_access
  end

  def pending?
    pending
  end
end
