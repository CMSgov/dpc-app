# frozen_string_literal: true

# Class for holding user email information from a CSP
class UserEmail < ApplicationRecord
  belongs_to :csp_user
  has_one :user, through: :csp_users

  # If we update this email to verified, make sure the others aren't.
  before_save :ensure_only_one_verified, if: -> { verified? && verified_changed? }

  private

  # Sets all of the user's other emails to not verified.
  def ensure_only_one_verified
    UserEmail.where(csp_user_id: csp_user_id)
             .where(verified: true)
             .where.not(id: id)
             .update_all(verified: false)
  end
end
