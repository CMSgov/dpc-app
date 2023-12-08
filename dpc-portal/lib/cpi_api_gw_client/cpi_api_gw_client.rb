# frozen_string_literal: true

require 'oauth2'

# A client for requests to the CPI API Gateway
class CPIAPIGatewayClient
  attr_accessor :access

  # rubocop:disable Metrics/MethodLength
  def initialize
    env = ENV.fetch('ENV', nil)
    client_id = ENV.fetch('CPI_API_GW_CLIENT_ID', nil)
    client_secret = ENV.fetch('CPI_API_GW_CLIENT_SECRET', nil)
    cms_idm_url = ENV.fetch('CMS_IDM_OAUTH_URL', nil)
    @cpi_api_gateway_url = ENV.fetch('CPI_API_GW_BASE_URL', nil)
    @client = OAuth2::Client.new(client_id, client_secret,
                                 site: cms_idm_url,
                                 token_url: '/oauth2/aus2151jb0hszrbLU297/v1/token',
                                 ssl: {
                                   verify: env != 'local'
                                 })
    fetch_token
  end
  # rubocop:enable Metrics/MethodLength

  def fetch_enrollment_id(npi)
    refresh_token
    body = { providerID: { npi: npi.to_s } }.to_json
    response = @access.post("#{@cpi_api_gateway_url}api/1.0/ppr/providers/enrollments",
                            headers: { 'Content-Type': 'application/json' },
                            body: body)
    response.parsed
  end

  def fetch_enrollment_roles(enrollment_id)
    refresh_token
    response = @access.get("#{@cpi_api_gateway_url}api/1.0/ppr/providers/enrollments/#{enrollment_id}/roles",
                           headers: { 'Content-Type': 'application/json' })
    response.parsed
  end

  # rubocop:disable Metrics/MethodLength
  def fetch_authorized_official_med_sanctions(ssn)
    refresh_token
    body = {
      providerID: {
        providerType: 'ind',
        identity: {
          idType: 'ssn',
          id: ssn.to_s
        }
      },
      dataSets: {
        subjectAreas: {
          medSanctions: true
        }
      }
    }.to_json
    response = @access.post("#{@cpi_api_gateway_url}api/1.0/ppr/providers",
                            headers: { 'Content-Type': 'application/json' },
                            body: body)
    response.parsed
  end
  # rubocop:enable Metrics/MethodLength

  private

  def fetch_token
    @access = @client.client_credentials.get_token(scope: 'READ')
  end

  def refresh_token
    return unless @access.expired?

    fetch_token
  end
end
