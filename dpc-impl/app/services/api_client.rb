# frozen_string_literal: true

class ApiClient
  attr_reader :base_url, :response_body, :response_status

  def initialize
    @base_url = ENV.fetch('API_METADATA_URL')
  end

  def create_client_token(imp_id, org_id, label)
    uri_string = base_url + '/Implementer/' + imp_id + '/Org/' + org_id + '/token'
    json = label.to_json
    post_request(uri_string, json)
  end

  def create_implementer(imp)
    uri_string = base_url + '/Implementer'
    json = {name: imp}.to_json
    post_request(uri_string, json)
    self
  end

  def create_provider_org(imp_id, npi)
    uri_string = base_url + '/Implementer/' + imp_id + '/org'
    json = {npi: npi}.to_json
    post_request(uri_string, json)
    self
  end

  def create_public_key(imp_id, org_id, params: {})
    uri_string = base_url + '/Implementer/' + imp_id + '/Org/' + org_id + '/key'
    json = params.to_json
    post_request(uri_string, json)
  end

  def create_system(imp_id, org_id, params: {})
    uri_string = base_url + '/Implementer/' + imp_id + '/Org/' + org_id + '/system'
    json = params.to_json
    post_request(uri_string, json)
  end

  def delete_public_key(imp_id, org_id, key_id)
    uri_string = base_url + '/Implementer/' + imp_id + '/Org/' + org_id + '/key/' + key_id
    delete_request(uri_string)
  end

  def get_tokens_keys(imp_id, provider_org_id)
    uri_string = base_url + '/Implementer/' + imp_id + '/Org/' + provider_org_id + '/system'
    get_request(uri_string)
  end

  def get_organization(org_id)
    uri_string = base_url + '/Organization/' + org_id
    get_request(uri_string)
  end

  def get_provider_orgs(imp_id)
    uri_string = base_url + '/Implementer/' + imp_id + '/org'
    get_request(uri_string)
  end

  def response_successful?
    (200...299).cover? @response_status
  end

  private

  def connection_error
    Rails.logger.warn 'Could not connect to API'
    @response_status = 500
    @response_body = { 'issue' => [{ 'details' => { 'text' => 'Connection error' }}]}
  end

  def delete_request(uri_string)
    uri = URI.parse uri_string
    request = Net::HTTP::Delete.new(uri.request_uri)

    http_request(request, uri)
  end

  def get_request(uri_string)
    uri = URI.parse uri_string
    request = Net::HTTP::Get.new(uri.request_uri)

    http_request(request, uri)
  end

  def http_request(request, uri)
    http = Net::HTTP.new(uri.host, uri.port)

    begin
      response = http.request(request)
      @response_status = response.code.to_i
    rescue => e
      connection_error
    else
      unless !response_successful?
        @response_body = parsed_response(response)
      else
        @response_body = response.body
      end
    end
  end

  def parsed_response(response)
    return self if response.body.blank?

    eval(response.body)
  end

  def post_request(uri_string, json)
    uri = URI.parse uri_string
    request = Net::HTTP::Post.new(uri.request_uri)
    request.body = json

    http_request(request, uri)
  end
end
