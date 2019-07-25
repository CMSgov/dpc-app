# frozen_string_literal: true

class DpcRegistration < ApplicationRecord
  belongs_to :user, inverse_of: :dpc_registration

  before_update :change_opt_in_status, if: :opt_in_changed?
  before_create :set_initialial_opt_in_status

  enum opt_in_status: { pending: 0, complete: 1 }
  enum access_level: { no_access: 0, synthesized: 1, production: 2 }

  validates :opt_in_status, inclusion: { in: opt_in_statuses.keys }
  validates :access_level, inclusion: { in: access_levels.keys }

  private

  def change_opt_in_status
    self.opt_in_status = 'pending'
  end

  def set_initialial_opt_in_status
    self.opt_in_status = opt_in ? 'pending' : 'complete'
  end
end
