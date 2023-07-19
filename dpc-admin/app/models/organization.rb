# frozen_string_literal: true

require './lib/luhnacy_lib/luhnacy_lib'

class Organization < ApplicationRecord
  include OrganizationsHelper
  include OrganizationTypable
  include Taggable

  has_many :taggings, as: :taggable
  has_many :tags, through: :taggings
  has_one :address, as: :addressable, dependent: :destroy
  has_many :organization_user_assignments, dependent: :destroy
  has_many :users, through: :organization_user_assignments
  has_one :registered_organization, dependent: :destroy

  enum organization_type: ORGANIZATION_TYPES

  validates :organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :name, uniqueness: true, presence: true
  validates :npi, uniqueness: { allow_blank: true }

  delegate :street, :street_2, :city, :state, :zip, to: :address, allow_nil: true, prefix: true
  accepts_nested_attributes_for :address, reject_if: :all_blank

  before_save :assign_id, if: -> { prod_sbx? }
  before_save :npi_valid?

  after_update :update_registered_organization

  scope :vendor, -> { where(organization_type: ORGANIZATION_TYPES['health_it_vendor']) }

  scope :provider, -> { where.not(organization_type: ORGANIZATION_TYPES['health_it_vendor']) }

  scope :is_registered, -> {
    where('id IN(SELECT DISTINCT(organization_id) FROM registered_organizations WHERE enabled IS true)')
  }

  scope :is_not_registered, -> {
    where('id IN(SELECT DISTINCT(o.id) FROM organizations AS o LEFT JOIN registered_organizations AS ro ON ro.organization_id = o.id WHERE ro.organization_id IS NULL OR ro.enabled IS NOT true)')
  }

  def address_type
    address&.address_type
  end

  def address_use
    address&.address_use
  end

  def npi=(input)
    super(input.blank? ? nil : input)
  end

  def assign_id
    return true if npi.present?

    self.npi = LuhnacyLib.generate_npi
  end

  def notify_users_of_sandbox_access
    organization_user_assignments.each(&:send_organization_sandbox_email)
  end

  def npi_valid?
    return npi.blank?

    validate_npi
  end

  def prod_sbx?
    ENV['ENV'] == 'prod-sbx'
  end

  def update_registered_organization
    return unless npi.present?

    return registered_organization.update_api_organization if registered_organization.present?
  end

  def reg_org
    return registered_organization if registered_organization.present?
  end

  def fhir_endpoint
    registered_organization.fhir_endpoint
  end
end

private

def validate_npi
  npi = '80840' + self.npi

  return if LuhnacyLib.validate_npi(npi)

  errors.add :npi, 'must be valid.'
  throw(:abort)
end
