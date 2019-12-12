# frozen_string_literal: true

class Organization < ApplicationRecord
  include OrganizationTypable

  has_one :address, as: :addressable
  has_many :organization_user_assignments, dependent: :destroy
  has_many :users, through: :organization_user_assignments
  has_many :fhir_endpoints
  has_many :registered_organizations

  enum organization_type: ORGANIZATION_TYPES

  validates :organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :name, uniqueness: true, presence: true
  validate :api_environments_allowed
  validates :npi, uniqueness: { allow_blank: true }

  delegate :street, :street_2, :city, :state, :zip, to: :address, allow_nil: true, prefix: true
  accepts_nested_attributes_for :address, :fhir_endpoints, reject_if: :all_blank

  after_save :update_registered_organizations

  scope :vendor, -> { where(organization_type: ORGANIZATION_TYPES['health_it_vendor']) }
  scope :provider, -> { where.not(organization_type: ORGANIZATION_TYPES['health_it_vendor']) }

  def address_type
    address&.address_type
  end

  def address_use
    address&.address_use
  end

  def api_environments=(input)
    input = [] unless input.is_a?(Array)

    self[:api_environments] = input.each_with_object([]) do |api_env, array|
      array << api_env.to_i unless api_env.blank?
    end
  end

  def npi=(input)
    super(input.blank? ? nil : input)
  end

  def api_credentialable?
    registered_organizations.count.positive? && npi.present?
  end

  def registered_api_envs
    registered_organizations.pluck(:api_env)
  end

  def notify_users_of_sandbox_access
    return unless sandbox_enabled?

    organization_user_assignments.each(&:send_organization_sandbox_email)
  end

  def update_registered_organizations
    return unless npi.present?

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
    return if api_environments.all? { |api_env| RegisteredOrganization.api_envs.value? api_env }

    errors.add(:api_environments, "must be in #{RegisteredOrganization.api_envs}")
  end
end
