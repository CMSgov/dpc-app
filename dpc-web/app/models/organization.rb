# frozen_string_literal: true

class Organization < ApplicationRecord
  include OrganizationTypable

  API_ENVIRONMENTS = {
    0 => 'sandbox'
  }.freeze

  has_one :address, as: :addressable
  has_many :organization_user_assignments
  has_many :users, through: :organization_user_assignments

  enum organization_type: ORGANIZATION_TYPES

  validates :organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :name, uniqueness: true, presence: true

  delegate :street, :street_2, :city, :state, :zip, to: :address, allow_nil: true, prefix: true
  accepts_nested_attributes_for :address

  def api_environments=(input)
    input = [] unless input.is_a?(Array)
    input.reject!(&:blank?)

    self[:api_environments] = input || []
  end
end
