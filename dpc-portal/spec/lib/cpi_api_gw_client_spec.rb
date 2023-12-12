# frozen_string_literal: true

require 'spec_helper'
require './lib/cpi_api_gw_client/cpi_api_gw_client'

describe CPIAPIGatewayClient do
  cpi_api_gateway_url = ENV.fetch('CPI_API_GW_BASE_URL', nil)
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

  describe '.fetch_enrollment' do
    it 'makes a post request' do
      oauth_client = instance_double(OAuth2::Client)
      client_credentials_strategy_instance = instance_double(OAuth2::Strategy::ClientCredentials)
      access_token_object_instance = instance_double(OAuth2::AccessToken)
      response_double = instance_double(OAuth2::Response)
      allow(OAuth2::Client).to receive(:new).and_return(oauth_client)
      allow(oauth_client).to receive(:client_credentials).and_return(client_credentials_strategy_instance)
      expect(client_credentials_strategy_instance).to receive(:get_token).and_return(access_token_object_instance)
      client = CPIAPIGatewayClient.new
      allow(client.access).to receive(:expired?).and_return(false)

      expect(client).to receive(:request_client).and_return access_token_object_instance
      expect(client.access).to receive(:post)
                           .with("#{cpi_api_gateway_url}api/1.0/ppr/providers/enrollments",
                                 { body: { providerID: { npi: '12345' } }.to_json,
                                   headers: { 'Content-Type': 'application/json' } })
        .and_return(response_double)
      expect(response_double).to receive(:parsed)
      client.fetch_enrollment(12_345)
    end
  end

  describe '.fetch_enrollment_roles' do
    it 'makes a get request' do
      oauth_client = instance_double(OAuth2::Client)
      client_credentials_strategy_instance = instance_double(OAuth2::Strategy::ClientCredentials)
      access_token_object_instance = instance_double(OAuth2::AccessToken)
      response_double = instance_double(OAuth2::Response)
      allow(OAuth2::Client).to receive(:new).and_return(oauth_client)
      allow(oauth_client).to receive(:client_credentials).and_return(client_credentials_strategy_instance)
      expect(client_credentials_strategy_instance).to receive(:get_token).and_return(access_token_object_instance)
      allow(access_token_object_instance).to receive(:expired?).and_return(false)
      client = CPIAPIGatewayClient.new

      expect(client).to receive(:request_client).and_return access_token_object_instance
      expect(client.access).to receive(:get)
                           .with("#{cpi_api_gateway_url}api/1.0/ppr/providers/enrollments/123456/roles",
                                 { headers: { 'Content-Type': 'application/json' } })
        .and_return(response_double)
      expect(response_double).to receive(:parsed)
      client.fetch_enrollment_roles(123_456)
    end
  end

  describe '.fetch_authorized_official_med_sanctions' do
    it 'makes a post request' do
      oauth_client = instance_double(OAuth2::Client)
      client_credentials_strategy_instance = instance_double(OAuth2::Strategy::ClientCredentials)
      access_token_object_instance = instance_double(OAuth2::AccessToken)
      response_double = instance_double(OAuth2::Response)
      allow(OAuth2::Client).to receive(:new).and_return(oauth_client)
      allow(oauth_client).to receive(:client_credentials).and_return(client_credentials_strategy_instance)
      expect(client_credentials_strategy_instance).to receive(:get_token).and_return(access_token_object_instance)
      allow(access_token_object_instance).to receive(:expired?).and_return(false)
      client = CPIAPIGatewayClient.new

      request_body = {
        providerID: {
          providerType: 'ind',
          identity: {
            idType: 'ssn',
            id: '111223456'
          }
        },
        dataSets: {
          subjectAreas: {
            medSanctions: true
          }
        }
      }

      expect(client).to receive(:request_client).and_return access_token_object_instance
      expect(client.access).to receive(:post)
                           .with("#{cpi_api_gateway_url}api/1.0/ppr/providers",
                                 { body: request_body.to_json,
                                   headers: { 'Content-Type': 'application/json' } })
        .and_return(response_double)
      expect(response_double).to receive(:parsed)
      client.fetch_authorized_official_med_sanctions(111_223_456)
    end
  end
end
