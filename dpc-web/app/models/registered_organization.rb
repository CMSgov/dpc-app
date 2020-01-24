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

  validates :api_id, :api_env, :organization, presence: true

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
end
