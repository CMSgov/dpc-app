# frozen_string_literal: true

class APIClient
  URLS = {
    'sandbox' => ENV.fetch('API_METADATA_URL_SANDBOX')
  }

  attr_reader :api_env

  def initialize(api_env)
    @api_env = api_env
  end

  def create_organization(org)
    build_sandbox_org_endpoint(org)

    uri_string = URLS[api_env] + '/Organization/$submit'
    json = OrganizationSubmitSerializer.new(org).to_json
    response = request(uri_string, json, golden_macaroon)
    parsed_response(response)
  end

  def delete_organization(org); end

  private

  def auth_header(token)
    { Authorization: "Bearer #{token}" }
  end

  def golden_macaroon
    @golden_macaroon ||= ENV.fetch("GOLDEN_MACAROON_#{api_env.upcase}")
  end

  def parsed_response(response)
    JSON.parse response
  end

  def request(uri_string, json, token)
    uri = URI.parse uri_string
    headers = { 'Content-Type': 'application/json' }.merge(auth_header(token))
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Post.new(uri.request_uri, headers)
    request.body = json
    http.request(request)
  end

  # Build but do not save
  # Necessary for sandbox?
  def build_sandbox_org_endpoint(org)
    return unless api_env == 'sandbox' && !org.profile_endpoint

    org.build_profile_endpoint status: 'Test', connection_type: 'hl7-fhir-rest',
                               name: 'DPC Sandbox Test Endpoint', address: 'https://dpc.cms.gov/test-endpoint'
  end
end
