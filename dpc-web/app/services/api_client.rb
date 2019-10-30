# frozen_string_literal: true

class APIClient
  URLS = {
    'sandbox' => 'https://sandbox.dpc.cms.gov/api/v1/metadata'
  }.freeze

  STUBBED_PROFILE_ENDPOINT = {
    status: 'Test',
    connection_type: 'hl7-fhir-rest',
    name: 'DPC Sandbox Test Endpoint',
    address: 'https://dpc.cms.gov'
  }.freeze

  attr_reader :api_env, :fhir_client

  def initialize(api_env)
    @api_env = api_env
    @fhir_client = FHIR::Client.new(URLS[api_env])
  end

  def create_organization(org)
    add_auth_header(golden_macaroon)

    api_org = FHIR::Organization.create(
      name: org.name,
      npi: org.npi,
      address: {
        use: org.address_use,
        type: org.address_type,
        line: org.address_street,
        city: org.address_city,
        state: org.address_state,
        postalCode: org.address_zip,
        country: 'US'
      },
      endpoint: profile_endpoint(org)
    )
    # org.update(api_org_id: api_org.id)
  end

  def delete_organization(org); end

  private

  def golden_macaroon
    @golden_macaroon ||= ENV.fetch("GOLDEN_MACAROON_#{api_env.upcase}")
  end

  def profile_endpoint(org)
    if api_env == 'sandbox' && org.profile_endpoint.nil?
      STUBBED_PROFILE_ENDPOINT
    else
      {
        status: org.profile_endpoint_status,
        connection_type: org.profile_endpoint_connection_type,
        name: org.profile_endpoint_name,
        address: org.profile_endpoint_uri
      }
    end
  end

  def add_auth_header(macaroon)
    fhir_client.additional_headers = { Authorization: "Bearer: Token #{macaroon}" }
  end
end
