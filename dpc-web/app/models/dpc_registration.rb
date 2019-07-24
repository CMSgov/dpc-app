# frozen_string_literal: true

class DpcRegistration < ApplicationRecord
  belongs_to :user, inverse_of: :dpc_registration

  before_update :change_opt_in_status, if: :opt_in_changed?
  before_create :set_initialial_opt_in_status

  enum opt_in_status: %i[pending complete]
  enum access_level: %i[no_access synthesized production]

  validates :opt_in_status, inclusion: { in: %w[pending complete] }
  validates :access_level, inclusion: { in: %w[no_access synthesized production] }

  private

  def change_opt_in_status
    self.opt_in_status = 'pending'
  end

  def set_initialial_opt_in_status
    self.opt_in_status = opt_in ? 'pending' : 'complete'
  end
end
