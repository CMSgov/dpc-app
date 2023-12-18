# frozen_string_literal: true

require 'oauth2'

# A client for requests to the CPI API Gateway
class CpiApiGatewayClient
  attr_accessor :access

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

  # fetch data about an organization, including enrollment_id
  def fetch_enrollment(npi)
    body = { providerID: { npi: npi.to_s } }.to_json
    response = request_client.post("#{@cpi_api_gateway_url}api/1.0/ppr/providers/enrollments",
                                   headers: { 'Content-Type': 'application/json' },
                                   body: body)
    response.parsed
  end

  # fetch a list of roles, roughly corresponding to associated individuals
  def fetch_enrollment_roles(enrollment_id)
    response = request_client.get("#{@cpi_api_gateway_url}api/1.0/ppr/providers/enrollments/#{enrollment_id}/roles",
                                  headers: { 'Content-Type': 'application/json' })
    response.parsed
  end

  # fetch info about the authorized official, including a list of med sanctions
  def fetch_authorized_official_med_sanctions(ssn)
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
    response = request_client.post("#{@cpi_api_gateway_url}api/1.0/ppr/providers",
                                   headers: { 'Content-Type': 'application/json' },
                                   body: body)
    response.parsed
  end

  private

  def fetch_token
    @access = @client.client_credentials.get_token(scope: 'READ')
  end

  def request_client
    fetch_token if @access.nil? || @access.expired?
    @access
  end
end
