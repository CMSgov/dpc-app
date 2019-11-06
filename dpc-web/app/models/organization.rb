# frozen_string_literal: true

class Organization < ApplicationRecord
  include OrganizationTypable

  has_one :address, as: :addressable
  has_many :organization_user_assignments
  has_many :users, through: :organization_user_assignments
  has_many :fhir_endpoints
  has_many :registered_organizations

  enum organization_type: ORGANIZATION_TYPES

  validates :organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :name, uniqueness: true, presence: true
  validate :api_environments_allowed
  validates :npi, presence: true, unless: -> { api_environments.empty? }

  delegate :street, :street_2, :city, :state, :zip, to: :address, allow_nil: true, prefix: true
  accepts_nested_attributes_for :address, :fhir_endpoints, reject_if: :all_blank

  after_save :update_registered_organizations

  def address_type
    address&.address_type
  end

  def address_use
    address&.address_use
  end

  def api_environments=(input)
    input = [] unless input.is_a?(Array)

    self[:api_environments] = input.inject([]) do |memo, el|
      memo << el.to_i unless el.blank?
      memo
    end
  end

  def registered_api_envs
    registered_organizations.pluck(:api_env)
  end

  def update_registered_organizations
    OrganizationRegistrar.delay.run(organization: self, api_environments: api_environment_strings)
  end

  def api_environment_strings
    RegisteredOrganization.api_envs.select do |_key, val|
      api_environments.include? val
    end.keys
  end

  def sandbox_enabled?
    api_environments.include?(0)
  end

  def api_environments_allowed
    return if api_environments.empty?

    unless api_environments.all? { |api_env| RegisteredOrganization.api_envs.value? api_env }
      errors.add(:api_environments, "must be in #{RegisteredOrganization.api_envs}")
    end
  end
end
