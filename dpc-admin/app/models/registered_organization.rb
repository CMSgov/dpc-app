# frozen_string_literal: true

class RegisteredOrganization < ApplicationRecord
  belongs_to :organization
  has_one :fhir_endpoint, dependent: :destroy

  before_create :create_api_organization
  after_create :notify_users_of_sandbox_access, if: -> { prod_sbx? }
  before_update :update_api_organization
  before_update :update_api_endpoint

  delegate :name, :status, :uri, to: :fhir_endpoint, allow_nil: true, prefix: true

  accepts_nested_attributes_for :fhir_endpoint

  validates :organization, presence: true

  def fhir_endpoint_id
    return unless api_endpoint_ref

    api_endpoint_ref.split('/')[1]
  end

  def client_tokens
    ClientTokenManager.new(registered_organization: self).client_tokens
  end

  def public_keys
    PublicKeyManager.new(registered_organization: self).public_keys
  end

  def create_api_organization
    api_request = api_service.create_organization(
      organization,
      fhir_endpoint: fhir_endpoint.attributes.slice('name', 'status', 'uri')
    )

    api_response = api_request.response_body

    if api_request.response_successful?
      self[:api_id] = api_response['id']
      self[:api_endpoint_ref] = api_response['endpoint'][0]['reference']
      api_response
    else
      action = 'registered'
      msg = organization.npi.present? ? api_response : 'NPI Required'
      api_error(action, msg)
      throw(:abort)
    end
  end

  def update_api_organization
    api_request = api_service.update_organization(organization, api_id, api_endpoint_ref)
    api_response = api_request.response_body
    return if api_request.response_successful?

    action = 'updated (organization)'
    msg = api_response
    api_error(action, msg)
    throw(:abort)
  end

  def update_api_endpoint
    api_request = api_service.update_endpoint(api_id, fhir_endpoint_id, fhir_endpoint)
    api_response = api_request.response_body
    return if api_request.response_successful?

    action = 'updated (endpoint)'
    msg = api_response
    api_error(action, msg)
    throw(:abort)
  end

  def build_default_fhir_endpoint
    build_fhir_endpoint(
      status: 'test',
      name: 'DPC Sandbox Test Endpoint',
      uri: 'https://dpc.cms.gov/test-endpoint'
    )
  end

  def notify_users_of_sandbox_access
    organization.notify_users_of_sandbox_access
  end

  def prod_sbx?
    ENV['ENV'] == 'prod-sbx'
  end

  private

  def api_service
    @api_service ||= DpcClient.new
  end

  def api_error(action, msg)
    errors.add(:base, "couldn't be #{action} with API: #{msg}")
  end
end
