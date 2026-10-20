# frozen_string_literal: true

# A verification session as reported by a CSP. Every outcome is stored, including failures and cancellations.
class CspUserVerification < ApplicationRecord
  belongs_to :csp
  belongs_to :user, optional: true
  belongs_to :invitation, optional: true
  has_many :csp_verification_checks, class_name: 'CspUserVerification::Check',
                                     inverse_of: :csp_user_verification, dependent: :delete_all

  validates :csp_verification_id, :status, presence: true

  def self.record!(csp:, verification:, user: nil, invitation: nil)
    raise ArgumentError, 'verification has no session_id' if verification[:session_id].blank?

    retried = false
    begin
      transaction { write_session(csp:, verification:, user:, invitation:) }
    rescue ActiveRecord::RecordNotUnique
      raise if retried

      retried = true
      retry
    end
  end

  # Locks and reloads an existing row first, so concurrent callbacks for the same session take turns: the second
  # waits, then sees what the first wrote, instead of both replacing the checks at once. Two callbacks that both find
  # no row compete to insert it, and the loser retries in record!.
  def self.write_session(csp:, verification:, user:, invitation:)
    record = find_or_initialize_by(csp:, csp_verification_id: verification[:session_id])
    record.lock! if record.persisted?
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

  # One check from a verification session.
  class Check < ApplicationRecord
    self.table_name = 'csp_verification_checks'

    belongs_to :csp_user_verification, inverse_of: :csp_verification_checks

    validates :check_name, presence: true
  end
end
