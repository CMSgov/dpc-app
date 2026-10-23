# frozen_string_literal: true

# A verification session as reported by a CSP. Every response is stored as its own row, including failures and
# cancellations, so a session can have several. The latest response for a session is the one with the latest
# created_at; session_created_at is when the CSP created the session.
class CspUserVerification < ApplicationRecord
  belongs_to :csp
  belongs_to :user, optional: true
  belongs_to :invitation, optional: true
  has_many :csp_verification_checks, inverse_of: :csp_user_verification, dependent: :delete_all

  validates :csp_verification_id, :status, presence: true

  def self.record!(csp:, verification:, user: nil, invitation: nil)
    raise ArgumentError, 'verification has no session_id' if verification[:session_id].blank?

    record = new(csp:, csp_verification_id: verification[:session_id], user:, invitation: (invitation unless user),
                 **verification.slice(:status, :session_created_at, :completed_at).compact)
    Array(verification[:checks]).each { |check| record.csp_verification_checks.build(check_attributes(check)) }
    record.save!
    record
  end

  def self.check_attributes(check)
    { check_name: check[:name], check_status: check[:status], status_details: check[:status_details],
      status_value: check[:value], reason_codes: check[:reason_codes] || [] }
  end
  private_class_method :check_attributes
end
