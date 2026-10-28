# frozen_string_literal: true

# One check from a verification session.
class CspVerificationCheck < ApplicationRecord
  belongs_to :csp_user_verification, inverse_of: :csp_verification_checks

  validates :check_name, presence: true
end
