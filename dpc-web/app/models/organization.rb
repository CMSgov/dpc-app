# frozen_string_literal: true

class Organization < ApplicationRecord
  include OrganizationTypable

  API_ENVIRONMENTS = {
    0 => 'Sandbox'
  }.freeze

  has_one :address, as: :addressable
  has_many :organization_user_assignments
  has_many :users, through: :organization_user_assignments
  has_one :profile_endpoint

  enum organization_type: ORGANIZATION_TYPES

  validates :organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :name, uniqueness: true, presence: true

  delegate :street, :street_2, :city, :state, :zip, to: :address, allow_nil: true, prefix: true
  accepts_nested_attributes_for :address, :profile_endpoint, reject_if: :all_blank

  before_save :update_api_organization

  def address_type
    address.address_type
  end

  def address_use
    address.address_use
  end

  def api_environments=(input)
    input = [] unless input.is_a?(Array)
    input.reject!(&:blank?)

    self[:api_environments] = input || []
  end

  def update_api_organization
    return unless api_environments_changed?

    added_envs = api_environments - api_environments_was
    removed_envs = api_environments_was - api_environments

    added_envs.each do |api_env|
      APIClient.new(api_env).create_organization(self)
    end

    removed_envs.each do |api_env|
      APIClient.new(api_env).delete_organization(self)
    end
  end
end
