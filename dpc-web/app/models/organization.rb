# frozen_string_literal: true

class Organization < ApplicationRecord
  include OrganizationTypable

  validates :name, uniqueness: true, presence: true

  def num_providers=(input)
    if input.blank?
      self[:num_providers] = 0
    end
  end
end
