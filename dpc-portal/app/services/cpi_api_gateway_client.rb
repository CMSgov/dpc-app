# frozen_string_literal: true

require 'oauth2'

# A client for requests to the CPI API Gateway
class CpiApiGatewayClient
  attr_accessor :access, :client

  def initialize
    env = ENV.fetch('ENV', nil)
    client_id = ENV.fetch('CPI_API_GW_CLIENT_ID', nil)
    client_secret = ENV.fetch('CPI_API_GW_CLIENT_SECRET', nil)
    cms_idm_url = ENV.fetch('CMS_IDM_OAUTH_URL', nil)
    @cpi_api_gateway_url = ENV.fetch('CPI_API_GW_BASE_URL', nil)
    @cpi_api_gateway_url += '/' unless @cpi_api_gateway_url.end_with?('/')
    @client = OAuth2::Client.new(client_id, client_secret,
                                 site: cms_idm_url,
                                 token_url: '/oauth2/aus2151jb0hszrbLU297/v1/token',
                                 ssl: {
                                   verify: env != 'local'
                                 })
    fetch_token
  end

  # fetch full enrollments information about an organization
  def fetch_profile(npi)
    url = "#{@cpi_api_gateway_url}api/1.0/ppr/providers/profile"
    start_tracking(:fetch_profile, url)
    body = { providerID: { npi: npi.to_s } }.to_json
    response = request_client.post(url,
                                   headers: { 'Content-Type': 'application/json' },
                                   body:)
    stop_tracking(:fetch_profile, url, response.status)
    response.parsed
  end

  # fetch info about the authorized official, including a list of med sanctions
  def fetch_med_sanctions_and_waivers_by_ssn(ssn)
    body = {
      providerID: {
        providerType: 'ind',
        identity: {
          idType: 'ssn',
          id: ssn.to_s
        }
      },
      dataSets: {
        all: true
      }
    }.to_json
    fetch_provider_info(body)
  end

  # fetch info about the organization
  def org_info(npi)
    body = {
      providerID: {
        providerType: 'org',
        npi: npi.to_s
      },
      dataSets: {
        all: true
      }
    }.to_json
    fetch_provider_info(body)
  end

  # The CPI API Gateway doesn't support a healthcheck, and their suggestion was to just hit one of their
  # end points and see if we get a response.  Don't over use this, as it counts against our rate limit.
  def healthcheck
    @client.client_credentials.get_token(scope: 'READ').nil?
    true
  rescue OAuth2::Error
    false
  end

  private

  def fetch_token
    @access = @client.client_credentials.get_token(scope: 'READ')
  end

  def request_client
    fetch_token if @access.nil? || @access.expired?
    @access
  end

  def fetch_provider_info(body)
    url = "#{@cpi_api_gateway_url}api/1.0/ppr/providers"
    start_tracking(:fetch_provider_info, url)
    response = request_client.post(url,
                                   headers: { 'Content-Type': 'application/json' },
                                   body:)
    stop_tracking(:fetch_provider_info, url, response.status)
    response.parsed
  end

  def start_tracking(method_name, url)
    @tracker = NewRelic::Agent::Tracer.start_external_request_segment(library: 'Net::HTTP', uri: url,
                                                                      procedure: :post)
    @start = Time.now
    Rails.logger.info(
      ['Calling CPI API Gateway',
       { cpi_api_gateway_request_method: :post,
         cpi_api_gateway_request_url: url,
         cpi_api_gateway_request_method_name: method_name }]
    )
  end

  def stop_tracking(method_name, url, code)
    Rails.logger.info(
      ['CPI API Gateway response info',
       { cpi_api_gateway_request_method: :post,
         cpi_api_gateway_request_url: url,
         cpi_api_gateway_request_method_name: method_name,
         cpi_api_gateway_response_status_code: code,
         cpi_api_gateway_response_duration: Time.now - @start }]
    )
    @tracker.finish
  end
end
