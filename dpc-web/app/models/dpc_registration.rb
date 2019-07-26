# frozen_string_literal: true

class DpcRegistration < ApplicationRecord
  belongs_to :user, inverse_of: :dpc_registration

  enum access_level: { no_access: 0, synthesized: 1, production: 2 }

  validates :access_level, inclusion: { in: access_levels.keys }
end
