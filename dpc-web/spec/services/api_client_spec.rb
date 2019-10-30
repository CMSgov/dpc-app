# frozen_string_literal: true

require 'rails_helper'

RSpec.describe APIClient do
  describe '#create_organization' do
    context 'without a profile endpoint persisted' do
      it 'sends stubbed profile endpoint for orgs in sandbox' do
        fhir_client = FHIR::Client.new(File.join(Rails.root, 'spec/fixtures/api_metadata.json'))
        allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')
        allow(FHIR::Client).to receive(:new).and_return(fhir_client)
        allow(fhir_client).to receive(:additional_headers=).with(Authorization: 'Bearer: Token 112233')
        allow(FHIR::Organization).to receive(:create)

        org = build(:organization)
        api_client = APIClient.new('sandbox')

        api_client.create_organization(org)

        expect(FHIR::Organization).to have_received(:create).with(
          {
            name: org.name,
            npi: org.npi,
            address: {
              use: org.address_use,
              type: org.address_type,
              line: org.address_street,
              city: org.address_city,
              state: org.address_state,
              postalCode: org.address_zip,
              country: 'US'
            },
            endpoint: {
              status: 'Test',
              connection_type: 'hl7-fhir-rest',
              name: 'DPC Sandbox Test Endpoint',
              address: 'https://dpc.cms.gov'
            }
          }
        )
      end

      # TODO what is response from API and test on handling errors
      # API should return 422 but doesn't yet, just ignores missing required fields
      it 'sends nil profile endpoint'
    end

    context 'with a profile endpoint persisted' do
      it 'sends full org data to API' do
        fhir_client = FHIR::Client.new(File.join(Rails.root, 'spec/fixtures/api_metadata.json'))
        allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')
        allow(FHIR::Client).to receive(:new).and_return(fhir_client)
        allow(fhir_client).to receive(:additional_headers=).with(Authorization: 'Bearer: Token 112233')
        allow(FHIR::Organization).to receive(:create)

        org = build(:organization)
        org.profile_endpoint = build(:profile_endpoint,
          name: 'Cool SBX',
          uri: 'https://cool.com',
          connection_type: 'hl7-fhir-rest',
          status: 'active'
        )
        api_client = APIClient.new('sandbox')

        api_client.create_organization(org)

        expect(FHIR::Organization).to have_received(:create).with(
          {
            name: org.name,
            npi: org.npi,
            address: {
              use: org.address_use,
              type: org.address_type,
              line: org.address_street,
              city: org.address_city,
              state: org.address_state,
              postalCode: org.address_zip,
              country: 'US'
            },
            endpoint: {
              status: 'active',
              connection_type: 'hl7-fhir-rest',
              name: 'Cool SBX',
              address: 'https://cool.com'
            }
          }
        )
      end
    end
  end
end