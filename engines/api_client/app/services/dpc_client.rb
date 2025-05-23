# frozen_string_literal: true

# Provides interaction with dpc-api
class DpcClient
  attr_reader :base_url, :admin_url, :response_body, :response_status

  def initialize
    @base_url = ENV.fetch('API_METADATA_URL')
    @admin_url = ENV.fetch('API_ADMIN_URL')
    @allow_invalid_ssl_cert = ActiveModel::Type::Boolean.new.cast(ENV.fetch('ALLOW_INVALID_SSL_CERT'))
  end

  def json_content
    'application/json'
  end

  def create_organization(org)
    uri_string = "#{base_url}/Organization/$submit"
    json = OrganizationSubmitSerializer.new(org).to_json
    post_request(uri_string, json, fhir_headers(golden_macaroon))
    self
  end

  def get_organization(api_id)
    uri_string = "#{base_url}/Organization/#{api_id}"
    org = get_fhir_request(uri_string, delegated_macaroon(api_id))
    response_successful? ? FHIR::Organization.new(org) : nil
  end

  def get_organization_by_npi(npi)
    uri_string = "#{base_url}/Admin/Organization?npis=npi|#{npi}"
    org = get_fhir_request(uri_string, golden_macaroon)
    response_successful? ? FHIR::Bundle.new(org) : nil
  end

  def update_organization(reg_org, api_id)
    fhir_org = FhirResourceBuilder.new.fhir_org(reg_org, api_id)
    update_fhir_request(api_id, fhir_org, api_id)
    self
  end

  def create_client_token(reg_org_api_id, params: {})
    uri_string = "#{base_url}/Token"

    json = params.to_json
    macaroon = delegated_macaroon(reg_org_api_id)
    post_request(uri_string, json, headers(macaroon))

    self
  end

  def delete_client_token(reg_org_api_id, token_id)
    uri_string = "#{base_url}/Token/#{token_id}"

    delete_request(uri_string, delegated_macaroon(reg_org_api_id))
  end

  def delete_public_key(reg_org_api_id, public_key_id)
    uri_string = "#{base_url}/Key/#{public_key_id}"

    delete_request(uri_string, delegated_macaroon(reg_org_api_id))
  end

  def get_client_tokens(reg_org_api_id)
    uri_string = "#{base_url}/Token"
    get_request(uri_string, delegated_macaroon(reg_org_api_id))
  end

  def create_public_key(reg_org_api_id, params: {})
    uri_string = "#{base_url}/Key"

    json = {
      key: params[:public_key],
      signature: params[:snippet_signature]
    }.to_json

    post_text_request(
      uri_string,
      json,
      { label: params[:label] },
      delegated_macaroon(reg_org_api_id)
    )

    self
  end

  def get_public_keys(reg_org_api_id)
    uri_string = "#{base_url}/Key"
    get_request(uri_string, delegated_macaroon(reg_org_api_id))
  end

  def create_ip_address(reg_org_api_id, params: {})
    post_text_request(
      "#{base_url}/IpAddress",
      { ip_address: params[:ip_address], label: params[:label] }.to_json,
      {},
      delegated_macaroon(reg_org_api_id)
    )
    self
  end

  def delete_ip_address(reg_org_api_id, addr_id)
    delete_request("#{base_url}/IpAddress/#{addr_id}", delegated_macaroon(reg_org_api_id))
  end

  def get_ip_addresses(reg_org_api_id)
    get_request("#{base_url}/IpAddress", delegated_macaroon(reg_org_api_id))
  end

  def healthcheck
    get_request("#{admin_url}/healthcheck", nil)
  end

  def response_successful?
    (200...299).cover? @response_status
  end

  private

  def auth_header(token)
    { Authorization: "Bearer #{token}" }
  end

  def delegated_macaroon(reg_org_api_id)
    m = Macaroon.from_binary(golden_macaroon)
    m.add_first_party_caveat("organization_id = #{reg_org_api_id}")
    m.add_first_party_caveat("expires = #{2.minutes.from_now.iso8601}")
    m.add_first_party_caveat('dpc_macaroon_version = 1')
    m.serialize
  end

  def golden_macaroon
    @golden_macaroon ||= ENV.fetch('GOLDEN_MACAROON')
  end

  def parsed_response(response)
    return self if response.body.blank?

    JSON.parse response.body
  end

  def delete_request(uri_string, token)
    uri = URI.parse uri_string
    request = Net::HTTP::Delete.new(uri.request_uri, headers(token))

    http_request(request, uri)
  end

  def get_request(uri_string, token)
    uri = URI.parse uri_string
    request = Net::HTTP::Get.new(uri.request_uri, headers(token))

    http_request(request, uri)
  end

  def get_fhir_request(uri_string, token)
    uri = URI.parse uri_string
    request = Net::HTTP::Get.new(uri.request_uri, fhir_headers(token))

    http_request(request, uri)
  end

  def post_request(uri_string, json, headers)
    uri = URI.parse uri_string
    request = Net::HTTP::Post.new(uri.request_uri, headers)
    request.body = json

    http_request(request, uri)
  end

  def post_text_request(uri_string, json, query_params, token)
    uri = URI.parse uri_string
    uri.query = URI.encode_www_form(query_params)
    text_headers = { 'Content-Type' => json_content, Accept: json_content }.merge(auth_header(token))

    request = Net::HTTP::Post.new(uri.request_uri, text_headers)
    request.body = json

    http_request(request, uri)
  end

  def http_request(request, uri)
    http = Net::HTTP.new(uri.host, uri.port)

    if use_ssl?(uri)
      http.use_ssl = true
      http.verify_mode = verify_mode
    end

    response = http.request(request)
    @response_status = response.code.to_i
    @response_body = response_successful? ? parsed_response(response) : response.body
  rescue StandardError => e
    # There are a whole bunch of errors that can get thrown if we're having network issues and we want to catch them all
    connection_error(e)
  end

  def update_fhir_request(reg_org_api_id, resource, resource_id)
    uri = URI.parse "#{base_url}/#{resource.resourceType}/#{resource_id}"
    request = Net::HTTP::Put.new(uri.request_uri, fhir_headers(delegated_macaroon(reg_org_api_id)))
    request.body = resource.to_json.to_s

    http_request(request, uri)
  end

  def headers(token)
    headers = { 'Content-Type' => json_content, Accept: json_content }
    token.nil? ? headers : headers.merge(auth_header(token))
  end

  def fhir_headers(token)
    { 'Content-Type' => 'application/fhir+json', Accept: 'application/fhir+json' }.merge(auth_header(token))
  end

  def use_ssl?(uri)
    !(Rails.env.development? || Rails.env.test?) && (uri.scheme == 'https')
  end

  def connection_error(error)
    Rails.logger.warn "Could not connect to API: #{error}"
    @response_status = 500
    @response_body = { 'issue' => [{ 'details' => { 'text' => 'Connection error' } }] }
  end

  def verify_mode
    return OpenSSL::SSL::VERIFY_NONE if @allow_invalid_ssl_cert

    OpenSSL::SSL::VERIFY_PEER
  end
end
