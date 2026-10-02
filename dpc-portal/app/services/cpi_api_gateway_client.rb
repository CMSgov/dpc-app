# frozen_string_literal: true

require 'oauth2'

# A client for requests to the CPI API Gateway
# rubocop:disable-next Metrics/ClassLength
class CpiApiGatewayClient
  attr_accessor :access, :client

  def initialize(
    cms_idm_url: ENV.fetch('CMS_IDM_OAUTH_URL', nil),
    cpi_api_gateway_url: ENV.fetch('CPI_API_GW_BASE_URL', nil)
  )
    env = ENV.fetch('ENV', nil)
    client_id = ENV.fetch('CPI_API_GW_CLIENT_ID', nil)
    client_secret = ENV.fetch('CPI_API_GW_CLIENT_SECRET', nil)
    token_endpoint = '/oauth2/aus2151jb0hszrbLU297/v1/token'
    ssl_verify = env != 'local'
    @cpi_api_gateway_url = cpi_api_gateway_url
    @cpi_api_gateway_url += '/' unless @cpi_api_gateway_url.end_with?('/')
    Rails.logger.info(
      ['CPI API Gateway client configuration',
       { cpi_api_gateway_token_endpoint: token_endpoint,
         cpi_api_gateway_url: @cpi_api_gateway_url,
         cpi_api_gateway_oauth_url: cms_idm_url,
         cpi_api_gateway_ssl_verify: ssl_verify }]
    )
    @client = OAuth2::Client.new(client_id, client_secret,
                                 site: cms_idm_url,
                                 token_url: token_endpoint,
                                 ssl: { verify: ssl_verify })
    fetch_token
  end

  # fetch full enrollments information about an organization
  def fetch_profile(npi)
    url = "#{@cpi_api_gateway_url}api/1.0/ppr/providers/profile"
    body = { providerID: { npi: npi.to_s } }.to_json
    start_tracking(:fetch_profile, url,
                   cpi_api_gateway_request_content_type: 'application/json',
                   cpi_api_gateway_request_body: body)
    response = request_client.post(url,
                                   headers: { 'Content-Type': 'application/json' },
                                   body:)
    stop_tracking(:fetch_profile, url, response.status)
    response.parsed
  rescue OAuth2::Error => e
    Rails.logger.error(
      ['CPI API Gateway fetch_profile failed',
       { cpi_api_gateway_request_url: url,
         cpi_api_gateway_request_npi: npi,
         cpi_api_gateway_request_body: body,
         cpi_api_gateway_response_status_code: e.response.status }]
    )
    raise
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
      dataSets: { all: true }
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
      dataSets: { all: true }
    }.to_json
    fetch_provider_info(body)
  end

  # The CPI API Gateway doesn't support a healthcheck, and their suggestion was to just hit one of their
  # end points and see if we get a response.  Don't over use this, as it counts against our rate limit.
  def healthy_api?
    # We'll get a 400 because the npi is bad, but any response indicates they're up and we're connected.
    org_info('fake_npi') && true
  rescue StandardError
    false
  end

  def healthy_auth?
    # Check if we can get a token
    fetch_token && true
  rescue StandardError
    false
  end

  private

  def fetch_token
    @access = @client.client_credentials.get_token(scope: 'READ')
    Rails.logger.info(
      ['Got CPI GW access token',
       { access_token_present: @access.token.present?,
         access_token_expired: @access.expired?,
         access_token_expires_at: @access.expires_at }]
    )
  end

  def request_client
    fetch_token if @access.nil? || @access.expired?
    @access
  end

  def fetch_provider_info(body)
    url = "#{@cpi_api_gateway_url}api/1.0/ppr/providers"
    start_tracking(:fetch_provider_info, url)

    # Build a custom span around the request for DD APM
    response = Datadog::Tracing.trace('cpi_api_gateway.request', resource: 'fetch_provider_info') do |span|
      span.type = 'http'
      span.set_tag('http.url', url)
      span.set_tag('http.method', 'POST')
      raw_response = request_client.post(url,
                                         headers: { 'Content-Type': 'application/json' },
                                         body:)
      span.set_tag('http.status_code', raw_response.status)
      raw_response
    end

    stop_tracking(:fetch_provider_info, url, response.status)
    response.parsed
  end

  def fetch_npi_from_pac_id(pac_id)
    url = "#{@cpi_api_gateway_url}api/1.0/ppr/providers/npinames"
    body = {
      providerID: {
        providerType: 'org',
        pacId: pac_id.to_s
      },
      dataSets: { all: true }
    }.to_json
    fetch_provider_info(body)

    # Build a custom span around the request for DD APM
    response = request_client.post(url, headers: { 'Content-Type': 'application/json' }, body:)
    response.parsed
  end

  def start_tracking(method_name, url, request_details = {})
    @start = Time.now
    Rails.logger.info(
      ['Calling CPI API Gateway',
       { cpi_api_gateway_request_method: :post,
         cpi_api_gateway_request_url: url,
         cpi_api_gateway_request_method_name: method_name }.merge(request_details)]
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
  end
end
