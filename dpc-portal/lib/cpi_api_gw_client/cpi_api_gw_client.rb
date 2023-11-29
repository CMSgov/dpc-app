# frozen_string_literal: true

require 'oauth2'

# A client for requests to the CPI API Gateway
class CPIAPIGatewayClient
  attr_accessor :token

  def initialize
    client_id = ENV.fetch('CPI_API_GW_CLIENT_ID', nil)
    client_secret = ENV.fetch('CPI_API_GW_CLIENT_SECRET', nil)
    @client = OAuth2::Client.new(client_id, client_secret,
                                 site: 'https://impl.idp.idm.cms.gov/',
                                 token_url: '/oauth2/aus2151jb0hszrbLU297/v1/token')
    fetch_token
  end

  private

  def fetch_token
    @token = @client.client_credentials.get_token
  end
end
