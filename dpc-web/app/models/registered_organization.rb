# frozen_string_literal: true

class RegisteredOrganization < ApplicationRecord
  belongs_to :organization
  has_one :fhir_endpoint, dependent: :destroy

  before_create :create_api_organization
  after_create :notify_users_of_sandbox_access, if: :sandbox?
  before_update :update_api_organization
  before_update :update_api_endpoint

  delegate :name, :status, :uri, to: :fhir_endpoint, allow_nil: true, prefix: true

  accepts_nested_attributes_for :fhir_endpoint

  enum api_env: {
    'sandbox' => 0,
    'production' => 1
  }

  validates :api_env, :organization, presence: true

  def fhir_endpoint_id
    return unless api_endpoint_ref

    api_endpoint_ref.split('/')[1]
  end

  def client_tokens
    ClientTokenManager.new(api_env: api_env, registered_organization: self).client_tokens
  end

  def public_keys
    PublicKeyManager.new(api_env: api_env, registered_organization: self).public_keys
  end

  def create_api_organization
    api_request = APIClient.new(api_env).create_organization(
      organization,
      fhir_endpoint: fhir_endpoint.attributes.slice('name', 'status', 'uri')
    )

    api_response = api_request.response_body

    if api_request.response_successful?
      self[:api_id] = api_response['id'],
      self[:api_endpoint_ref] = api_response['endpoint'][0]['reference']
      api_response
    else
      errors.add(:base, "couldn't be registered with #{api_env} API: #{api_response}")
      throw(:abort)
    end
  end

  def update_api_organization
    api_request = APIClient.new(api_env).update_organization(self)
    api_response = api_request.response_body

    unless api_request.response_successful?
      errors.add(:base, "couldn't be registered with #{api_env} API (organization update): #{api_response}")
      throw(:abort)
    end
  end

  def update_api_endpoint
    api_request = APIClient.new(api_env).update_endpoint(self)
    api_response = api_request.response_body

    unless api_request.response_successful?
      errors.add(:base, "couldn't be registered with #{api_env} API (endpoint update): #{api_response}")
      throw(:abort)
    end
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
end
