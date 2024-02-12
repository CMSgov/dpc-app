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
      organization_npi = 1_234_554_333
      ao_ssn = 111_223_456
      expect(cpi_api_gw_client).to receive(:fetch_authorized_official_med_sanctions)
        .with(ao_ssn)
        .and_return({
                      'provider' => {
                        'providerType' => 'ind',
                        'idType' => 'ssn',
                        'npi' => '4444444444',
                        'medSanctions' => [
                          {
                            'sanctionCode' => '12ABC',
                            'sanctionDate' => '2010-08-17',
                            'description' => 'MED Sanction',
                            'deletedTimestamp' => '2021-09-24T16:26:48.598-04:00',
                            'reinstatementDate' => '2020-08-17',
                            'reinstatementReasonDescription' => 'LICENSE REVOCATION OR SUSPENSION'
                          }
                        ]
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_enrollment)
        .with(organization_npi)
        .and_return({
                      'enrollments' => [{
                        'enrollmentID' => '023456',
                        'status' => 'APPROVED',
                        'statusDate' => '2024-02-06',
                        'npi' => organization_npi.to_s
                      },
                                        {
                                          'enrollmentID' => '045678',
                                          'status' => 'APPROVED',
                                          'statusDate' => '2023-02-06',
                                          'npi' => organization_npi.to_s
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
                          'ssn' => ao_ssn.to_s
                        }]
                      }
                    })
      service.check_ao_eligibility(organization_npi, ao_ssn)
    end

    it 'returns an error if the AO has an active med sanction' do
      service = AOVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = 111_223_456
      expect(cpi_api_gw_client).to receive(:fetch_authorized_official_med_sanctions)
        .with(ao_ssn)
        .and_return({
                      'provider' => {
                        'providerType' => 'ind',
                        'idType' => 'ssn',
                        'npi' => '4444444444',
                        'medSanctions' => [
                          {
                            'sanctionCode' => '12ABC',
                            'sanctionDate' => '2010-08-17',
                            'description' => 'MED Sanction',
                            'deletedTimestamp' => '2021-09-24T16:26:48.598-04:00',
                            'reinstatementDate' => nil,
                            'reinstatementReasonDescription' => 'LICENSE REVOCATION OR SUSPENSION'
                          }
                        ]
                      }
                    })
      response = service.check_ao_eligibility(organization_npi, ao_ssn)
      expect(response).to include({ success: false, reason: 'med_sanctions' })
    end

    it 'returns an error if looking up enrollments for the NPI returns a 404' do
      service = AOVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = 111_223_456
      allow(cpi_api_gw_client).to receive(:fetch_authorized_official_med_sanctions)
        .with(ao_ssn)
        .and_return({
                      'provider' => {
                        'providerType' => 'ind',
                        'idType' => 'ssn',
                        'npi' => '4444444444'
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_enrollment)
                               .with(organization_npi)
        .and_return({
                      'code' => '404'
                    })
      response = service.check_ao_eligibility(organization_npi, ao_ssn)
      expect(response).to include({ success: false, reason: 'bad_npi' })
    end

    it 'returns an error if there are no approved enrollments' do
      service = AOVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = 111_223_456
      allow(cpi_api_gw_client).to receive(:fetch_authorized_official_med_sanctions)
        .with(ao_ssn)
        .and_return({
                      'provider' => {
                        'providerType' => 'ind',
                        'idType' => 'ssn',
                        'npi' => '4444444444'
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_enrollment)
        .with(organization_npi)
        .and_return({
                      'enrollments' => [{
                        'enrollmentID' => '023456',
                        'status' => 'DEACTIVATED',
                        'statusDate' => '2024-02-06',
                        'npi' => organization_npi.to_s
                      },
                                        {
                                          'enrollmentID' => '045678',
                                          'status' => 'DEACTIVATED',
                                          'statusDate' => '2023-02-06',
                                          'npi' => organization_npi.to_s
                                        }]
                    })
      response = service.check_ao_eligibility(organization_npi, ao_ssn)
      expect(response).to include({ success: false, reason: 'no_approved_enrollment' })
    end

    it 'returns an error if the user is not an authorized official' do
      service = AOVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = 111_223_456
      enrollment_id = '023456'
      allow(cpi_api_gw_client).to receive(:fetch_authorized_official_med_sanctions)
        .with(ao_ssn)
        .and_return({
                      'provider' => {
                        'providerType' => 'ind',
                        'idType' => 'ssn',
                        'npi' => '4444444444'
                      }
                    })
      allow(cpi_api_gw_client).to receive(:fetch_enrollment)
        .with(organization_npi)
        .and_return({
                      'enrollments' => [{
                        'enrollmentID' => enrollment_id,
                        'status' => 'APPROVED',
                        'statusDate' => '2024-02-06',
                        'npi' => organization_npi.to_s
                      },
                                        {
                                          'enrollmentID' => '045678',
                                          'status' => 'DEACTIVATED',
                                          'statusDate' => '2023-02-06',
                                          'npi' => organization_npi.to_s
                                        }]
                    })
      expect(cpi_api_gw_client).to receive(:fetch_enrollment_roles)
        .with(enrollment_id)
        .and_return({
                      'enrollments' => {
                        'enrollmentID' => enrollment_id,
                        'roles' => [{
                          'macID' => '12345',
                          'ssn' => ao_ssn,
                          'roleCode' => '42'
                        }]
                      }
                    })
      response = service.check_ao_eligibility(organization_npi, ao_ssn)
      expect(response).to include({ success: false, reason: 'user_not_authorized_official' })
    end
  end
end
