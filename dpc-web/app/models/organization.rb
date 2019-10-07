# frozen_string_literal: true

class Organization < ApplicationRecord
  include OrganizationTypable

  has_one :address, as: :addressable

  enum organization_type: ORGANIZATION_TYPES

  validates :organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }

  validates :name, uniqueness: true, presence: true

  delegate :street, :street_2, :city, :state, :zip, to: :address, allow_nil: true, prefix: true
  accepts_nested_attributes_for :address
end
