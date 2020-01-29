# frozen_string_literal: true

class RegisteredOrganization < ApplicationRecord
  belongs_to :organization
  has_one :fhir_endpoint, dependent: :destroy

  delegate :name, :status, :uri, to: :fhir_endpoint, allow_nil: true, prefix: true

  accepts_nested_attributes_for :fhir_endpoint

  enum api_env: {
    'sandbox' => 0,
    'production' => 1
  }

  validates :api_env, :organization, presence: true

  def create_api_organization
    api_request = APIClient.new(api_env).create_organization(
      organization,
      fhir_endpoint: fhir_endpoint.attributes
    )

    api_response = api_request.response_body

    if api_request.response_successful?
      update_api_attributes!(api_response)
      organization.notify_users_of_sandbox_access if sandbox?
    end

    api_response
  end

  def update_api_organization
    if api_id.nil?
      create_api_organization
    else
      api_request = APIClient.new(api_env).update_organization(self)
      api_request.response_body
    end
  end

  def client_tokens
    ClientTokenManager.new(api_env: api_env, organization: organization).client_tokens
  end

  def public_keys
    PublicKeyManager.new(api_env: api_env, organization: organization).public_keys
  end

  def build_default_fhir_endpoint
    build_fhir_endpoint(
      status: 'test',
      name: 'DPC Sandbox Test Endpoint',
      uri: 'https://dpc.cms.gov/test-endpoint'
    )
  end

  private

  def update_api_attributes!(api_response)
    update!(
      api_id: api_response['id'],
      api_endpoint_ref: api_response['endpoint'][0]['reference']
    )
  end
end
