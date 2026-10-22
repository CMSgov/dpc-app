# frozen_string_literal: true

# A verification session as reported by a CSP. Every outcome is stored, including failures and cancellations.
class CspUserVerification < ApplicationRecord
  belongs_to :csp
  belongs_to :user, optional: true
  belongs_to :invitation, optional: true
  has_many :csp_verification_checks, inverse_of: :csp_user_verification, dependent: :delete_all

  validates :csp_verification_id, :status, presence: true

  def self.record!(csp:, verification:, user: nil, invitation: nil)
    raise ArgumentError, 'verification has no session_id' if verification[:session_id].blank?

    transaction { write_session(csp:, verification:, user:, invitation:) }
  end

  def self.write_session(csp:, verification:, user:, invitation:)
    record = find_or_create_by(csp:, csp_verification_id: verification[:session_id]) do |new_record|
      new_record.status = verification[:status]
    end
    record.assign_attributes(verification.slice(:status, :created_at, :completed_at).compact)
    record.link(user:, invitation:)
    record.save!
    record.replace_checks(verification[:checks]) if verification[:checks].present?
    record
  end

  # Links the row to the existing user or to the invitation they are registering with. Only the first link is kept.
  def link(user:, invitation:)
    return if user_id.present? || invitation_id.present?

    if user
      self.user = user
    else
      self.invitation = invitation
    end
  end

  def replace_checks(checks)
    csp_verification_checks.delete_all
    checks.each do |check|
      csp_verification_checks.create!(check_name: check[:name], check_status: check[:status],
                                      status_details: check[:status_details], status_value: check[:value],
                                      reason_codes: check[:reason_codes] || [], completed_at: check[:completed_at])
    end
  end
end
