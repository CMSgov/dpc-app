# frozen_string_literal: true

# Class for holding user email information from a CSP
class UserEmail < ApplicationRecord
  belongs_to :csp_user
  has_one :user, through: :csp_users

  # If we update this email to primary, make sure the others aren't.
  before_save :ensure_only_one_primary, if: -> { primary? && primary_changed? }

  private

  # Sets all of the user's other emails to not primary.
  def ensure_only_one_primary
    UserEmail.where(csp_user_id: csp_user_id)
             .where(primary: true)
             .where.not(id: id)
             .update_all(primary: false)
  end
end
