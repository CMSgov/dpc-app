# frozen_string_literal: true

require 'spec_helper'
require './lib/cpi_api_gw_client/cpi_api_gw_client'

describe CPIAPIGatewayClient do
  describe '.new' do
    it 'sets a token' do
      oauth_client = instance_double(OAuth2::Client)
      client_credentials_strategy_instance = instance_double(OAuth2::Strategy::ClientCredentials)
      access_token_object_instance = instance_double(OAuth2::AccessToken)
      allow(OAuth2::Client).to receive(:new).and_return(oauth_client)
      allow(oauth_client).to receive(:client_credentials).and_return(client_credentials_strategy_instance)
      expect(client_credentials_strategy_instance).to receive(:get_token).and_return(access_token_object_instance)
      client = CPIAPIGatewayClient.new

      expect(client.access).to eq access_token_object_instance
    end
  end
end
