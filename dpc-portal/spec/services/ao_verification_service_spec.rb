# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'
require './app/services/ao_verification_service'
require './app/services/cpi_api_gateway_client'

describe AoVerificationService do
  let(:ao_verification_service) { instance_double(AoVerificationService) }
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
      allow(AoVerificationService).to receive(:new).and_return(ao_verification_service)
      service = AoVerificationService.new
      expect(service).to eq ao_verification_service
    end
  end

  describe '.check_ao_eligibility' do
    it 'makes api calls to CPI API Gateway and returns success message' do
      service = AoVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = '111223456'
      hashed_ao_ssn = Digest::SHA2.new(256).hexdigest(ao_ssn)
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
                          'ssn' => ao_ssn
                        }]
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_med_sanctions_and_waivers)
        .with(ao_ssn)
        .and_return({
                      'provider' => {
                        'providerType' => 'ind',
                        'idType' => 'ssn',
                        'npi' => '4444444444'
                      }
                    })
      response = service.check_ao_eligibility(organization_npi, hashed_ao_ssn)
      expect(response).to include({ success: true })
    end

    it 'returns an error if looking up enrollments for the NPI returns a 404' do
      service = AoVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = '111223456'
      hashed_ao_ssn = Digest::SHA2.new(256).hexdigest(ao_ssn)
      expect(cpi_api_gw_client).to receive(:fetch_enrollment)
                               .with(organization_npi)
        .and_return({ 'code' => '404' })
      response = service.check_ao_eligibility(organization_npi, hashed_ao_ssn)
      expect(response).to include({ success: false, reason: 'bad_npi' })
    end

    it 'returns an error if there are no approved enrollments' do
      service = AoVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = '111223456'
      hashed_ao_ssn = Digest::SHA2.new(256).hexdigest(ao_ssn)
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
      response = service.check_ao_eligibility(organization_npi, hashed_ao_ssn)
      expect(response).to include({ success: false, reason: 'no_approved_enrollment' })
    end

    it 'returns an error if the user is not an authorized official' do
      service = AoVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = '111223456'
      hashed_ao_ssn = Digest::SHA2.new(256).hexdigest(ao_ssn)
      enrollment_id = '023456'
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
      response = service.check_ao_eligibility(organization_npi, hashed_ao_ssn)
      expect(response).to include({ success: false, reason: 'user_not_authorized_official' })
    end

    it 'returns an error if the AO has an active med sanction' do
      service = AoVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = '111223456'
      hashed_ao_ssn = Digest::SHA2.new(256).hexdigest(ao_ssn)
      enrollment_id = '023456'
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
                          'roleCode' => '10'
                        }]
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_med_sanctions_and_waivers)
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
      response = service.check_ao_eligibility(organization_npi, hashed_ao_ssn)
      expect(response).to include({ success: false, reason: 'med_sanctions' })
    end

    it 'does not return an error if user has a med sanction AND waiver' do
      service = AoVerificationService.new
      organization_npi = 1_234_554_333
      ao_ssn = '111223456'
      hashed_ao_ssn = Digest::SHA2.new(256).hexdigest(ao_ssn)
      enrollment_id = '023456'
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
      allow(cpi_api_gw_client).to receive(:fetch_enrollment_roles)
        .with(enrollment_id)
        .and_return({
                      'enrollments' => {
                        'enrollmentID' => enrollment_id,
                        'roles' => [{
                          'macID' => '12345',
                          'ssn' => ao_ssn,
                          'roleCode' => '10'
                        }]
                      }
                    })
      expect(cpi_api_gw_client).to receive(:fetch_med_sanctions_and_waivers)
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
                        ],
                        'waiverInfo' => [
                          {
                            'effectiveDate' => Date.today.last_year.to_s,
                            'endDate' => Date.today.next_year.to_s,
                            'comment' => 'Waiver covers everything'
                          }
                        ]
                      }
                    })
      response = service.check_ao_eligibility(organization_npi, hashed_ao_ssn)
      expect(response).to include({ success: true })
    end
  end
end
