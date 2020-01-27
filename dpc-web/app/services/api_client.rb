# frozen_string_literal: true

class APIClient
  attr_reader :api_env, :base_url, :response_body, :response_status

  def initialize(api_env)
    @api_env = api_env
    @base_url = base_urls[api_env]
  end

  def create_organization(org)
    uri_string = base_url + '/Organization/$submit'
    json = OrganizationSubmitSerializer.new(org).to_json
    post_request(uri_string, json, fhir_headers(golden_macaroon))
    self
  end

  def get_organization(reg_org)
    client = FHIR::Client.new(base_url)
    client.additional_headers = auth_header(delegated_macaroon(reg_org.api_id))
    client.read(FHIR::Organization, reg_org.api_id).resource
  end

  def update_organization(reg_org)
    fhir_org = FhirResourceBuilder.new.fhir_org(reg_org)

    fhir_client_update_request(reg_org.api_id, fhir_org, reg_org.api_id)
  end

  def delete_organization(reg_org)
    fhir_client.additional_headers = auth_header(delegated_macaroon(reg_org.api_id))

    response = fhir_client.destroy(FHIR::Organization, reg_org.api_id)

    if response.response[:code] == '200'
      true
    else
      Rails.logger.warn 'Unsuccessulful request to API'
      @response_status = response.response[:code]
      @response_body = { 'issue' => [{ 'details' => { 'text' => 'Request error' } }] }
      false
    end
  end

  def update_endpoint(reg_org)
    fhir_endpoint = FhirResourceBuilder.new.fhir_endpoint(reg_org)

    fhir_client_update_request(reg_org.api_id, fhir_endpoint, fhir_endpoint.id)
  end

  def create_client_token(reg_org_id, params: {})
    uri_string = base_url + '/Token'

    json = params.to_json
    macaroon = delegated_macaroon(reg_org_id)
    post_request(uri_string, json, headers(macaroon))

    self
  end

  def get_client_tokens(reg_org_id)
    uri_string = base_url + '/Token'
    get_request(uri_string, delegated_macaroon(reg_org_id))
  end

  def create_public_key(reg_org_id, params: {})
    uri_string = base_url + '/Key'

    post_text_request(
      uri_string,
      params[:public_key],
      { label: params[:label] },
      delegated_macaroon(reg_org_id)
    )

    self
  end

  def get_public_keys(reg_org_id)
    uri_string = base_url + '/Key'
    get_request(uri_string, delegated_macaroon(reg_org_id))
  end

  def response_successful?
    @response_status == 200
  end

  def fhir_client
    @fhir_client ||= FHIR::Client.new(base_url)
  end

  private

  def base_urls
    {
      'sandbox' => ENV.fetch('API_METADATA_URL_SANDBOX')
    }
  end

  def auth_header(token)
    { 'Authorization': "Bearer #{token}" }
  end

  def delegated_macaroon(reg_org_api_id)
    m = Macaroon.from_binary(golden_macaroon)
    m.add_first_party_caveat("organization_id = #{reg_org_api_id}")
    m.add_first_party_caveat("expires = #{2.minutes.from_now.iso8601}")
    m.add_first_party_caveat('dpc_macaroon_version = 1')
    m.serialize
  end

  def golden_macaroon
    @golden_macaroon ||= ENV.fetch("GOLDEN_MACAROON_#{api_env.upcase}")
  end

  def parsed_response(response)
    JSON.parse response.body
  end

  def get_request(uri_string, token)
    uri = URI.parse uri_string
    request = Net::HTTP::Get.new(uri.request_uri, headers(token))

    http_request(request, uri)
  end

  def post_request(uri_string, json, headers)
    uri = URI.parse uri_string
    request = Net::HTTP::Post.new(uri.request_uri, headers)
    request.body = json

    http_request(request, uri)
  end

  def post_text_request(uri_string, text, query_params, token)
    uri = URI.parse uri_string
    uri.query = URI.encode_www_form(query_params)
    text_headers = { 'Content-Type': 'text/plain', 'Accept': 'application/json' }.merge(auth_header(token))

    request = Net::HTTP::Post.new(uri.request_uri, text_headers)
    request.body = text

    http_request(request, uri)
  end

  def http_request(request, uri)
    http = Net::HTTP.new(uri.host, uri.port)

    if use_ssl?
      http.use_ssl = true
      http.verify_mode = OpenSSL::SSL::VERIFY_NONE unless verify_ssl_cert?
    end

    response = http.request(request)
    @response_status = response.code.to_i
    @response_body = parsed_response(response)
  rescue Errno::ECONNREFUSED
    Rails.logger.warn 'Could not connect to API'
    @response_status = 500
    @response_body = { 'issue' => [{ 'details' => { 'text' => 'Connection error' } }] }
  end

  def fhir_client_update_request(reg_org_id, resource, resource_id)
    fhir_client.additional_headers = auth_header(delegated_macaroon(reg_org_id))
    response = fhir_client.update(resource, resource_id)

    if response.response[:code] == '200'
      true
    else
      Rails.logger.warn 'Unsuccessulful request to API'
      @response_status = response.response[:code]
      @response_body = { 'issue' => [{ 'details' => { 'text' => 'Request error' } }] }
      false
    end
  end

  def headers(token)
    { 'Content-Type': 'application/json', 'Accept': 'application/json' }.merge(auth_header(token))
  end

  def fhir_headers(token)
    { 'Content-Type': 'application/fhir+json', 'Accept': 'application/fhir+json' }.merge(auth_header(token))
  end

  def use_ssl?
    !(Rails.env.development? || Rails.env.test?)
  end

  def verify_ssl_cert?
    ENV.fetch('VERIFY_SSL_CERT') != 'false'
  end
end
