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

  def create_client_token(registered_org, params: {})
    uri_string = base_urls[api_env] + "/Token/#{registered_org.api_id}"

    json = params.to_json
    response = request(uri_string, json, delegated_macaroon)

    @response_status = response.code.to_i
    @response_body = parsed_response(response)

    self
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

  def delegated_macaroon
  end

  def golden_macaroon
    @golden_macaroon ||= ENV.fetch("GOLDEN_MACAROON_#{api_env.upcase}")
  end

  def parsed_response(response)
    JSON.parse response.body
  end

  def post_request(uri_string, json, token)
    uri = URI.parse uri_string
    headers = { 'Content-Type': 'application/json' }.merge(auth_header(token))
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Post.new(uri.request_uri, headers)
    request.body = json

    response = http.request(request)

    @response_status = response.code.to_i
    @response_body = parsed_response(response)
  end
end
