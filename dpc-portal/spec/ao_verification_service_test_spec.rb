# frozen_string_literal: true

require 'spec_helper'
require './app/services/ao_verification_service'
require './app/services/cpi_api_gateway_client'

describe AOVerificationService do
  let(:ao_verification_service) { instance_double(AOVerificationService) }
  let(:mock_oauth2_client) { instance_double(OAuth2::Client) }
  let(:mock_credentials) { instance_double(OAuth2::Strategy::ClientCredentials) }
  let(:mock_access_token) { instance_double(OAuth2::AccessToken) }
  let(:cpi_api_gw_client) { instance_double(CpiApiGatewayClient) }

  before do
    allow(OAuth2::Client).to receive(:new).and_return(mock_oauth2_client)
    allow(mock_oauth2_client).to receive(:client_credentials).and_return(mock_credentials)
    allow(mock_credentials).to receive(:get_token).and_return(mock_access_token)
    allow(mock_access_token).to receive(:expired?).and_return(false)
    allow(CpiApiGatewayClient).to receive(:new).and_return(cpi_api_gw_client)
  end

  describe '.new' do
    it 'initializes a new instance' do
      allow(AOVerificationService).to receive(:new).and_return(ao_verification_service)
      service = AOVerificationService.new
      expect(service).to eq ao_verification_service
    end
  end

  describe '.check_ao_eligibility' do
    it 'makes api calls to CPI API Gateway' do
      service = AOVerificationService.new
      npi = 123_455_433
      ssn = 111_223_456
      expect(cpi_api_gw_client).to receive(:fetch_enrollment)
        .with(npi)
        .and_return({
                      'enrollments' => [{
                        'enrollmentID' => '023456',
                        'status' => 'APPROVED',
                        'statusDate' => '2024-02-06',
                        'npi' => npi.to_s
                      },
                                        {
                                          'enrollmentID' => '045678',
                                          'status' => 'APPROVED',
                                          'statusDate' => '2023-02-06',
                                          'npi' => npi.to_s
                                        }]
                    })
      expect(cpi_api_gw_client).to receive(:fetch_enrollment_roles)
        .with('023456')
        .and_return({
                      'enrollments' => {
                        'enrollmentID' => '023456',
                        'roles' => [{
                          'roleCode' => '12',
                          'ein' => '43563'
                        },
                                    {
                                      'roleCode' => '10',
                                      'ssn' => '4444'
                                    }]
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_enrollment_roles)
        .with('045678')
        .and_return({
                      'enrollments' => {
                        'enrollmentID' => '045678',
                        'roles' => [{
                          'roleCode' => '10',
                          'ssn' => ssn.to_s
                        }]
                      }
                    })
      service.check_ao_eligibility(npi, ssn)
    end
  end
end
