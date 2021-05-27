# frozen_string_literal: true

class ApiClient
  attr_reader :base_url, :response_body, :response_status

  def initialize
    @base_url = ENV.fetch('API_METADATA_URL')
  end

  def create_implementer(imp)
    uri_string = base_url + '/Implementer'
    json = {name: imp}.to_json
    post_request(uri_string, json)
    self
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

  def http_request(request, uri)
    http = Net::HTTP.new(uri.host, uri.port)

    begin
      response = http.request(request)
    rescue => e
      connection_error
    else
      @response_status = response.code.to_i
      @response_body = parsed_response(response)
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
