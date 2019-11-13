# frozen_string_literal: true

class APIClient
  attr_reader :api_env, :response_body, :response_status

  def initialize(api_env)
    @api_env = api_env
  end

  def create_organization(org)
    uri_string = base_urls[api_env] + '/Organization/$submit'
    json = OrganizationSubmitSerializer.new(org).to_json
    post_request(uri_string, json, golden_macaroon)
    self
  end

  def delete_organization(org); end

  def create_client_token(reg_org_id, params: {})
    uri_string = base_urls[api_env] + "/Token"

    json = params.to_json
    post_request(uri_string, json, delegated_macaroon(reg_org_id))

    self
  end

  def get_client_tokens(reg_org_id)
    uri_string = base_urls[api_env] + "/Token"
    get_request(uri_string, delegated_macaroon(reg_org_id))
  end

  def response_successful?
    @response_status == 200
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

  def delegated_macaroon(reg_org_id)
    # Temp fix
    decoded = Base64.decode64(golden_macaroon)
    m = Macaroon.from_binary(decoded)
    m.add_first_party_caveat("organization_id = #{reg_org_id}")
    m.add_first_party_caveat("expires = #{2.minutes.from_now.iso8601}")
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

  def post_request(uri_string, json, token)
    uri = URI.parse uri_string
    request = Net::HTTP::Post.new(uri.request_uri, headers(token))
    request.body = json

    http_request(request, uri)
  end

  def http_request(request, uri)
    http = Net::HTTP.new(uri.host, uri.port)

    response = http.request(request)

    @response_status = response.code.to_i
    @response_body = parsed_response(response)
  end

  def headers(token)
    { 'Content-Type': 'application/json' }.merge(auth_header(token))
  end
end
