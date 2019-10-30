# frozen_string_literal: true

require 'rails_helper'

RSpec.describe APIClient do
  describe '#create_organization' do
    context 'without a profile endpoint persisted' do
      it 'sends stubbed profile endpoint for orgs in sandbox' do
        fhir_client = instance_double(FHIR::Client)
        allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')
        allow(FHIR::Client).to receive(:new).and_return(fhir_client)
        allow(fhir_client).to receive(:additional_headers=).with(Authorization: 'Bearer: Token 112233')
        allow(FHIR::Organization).to receive(:create)

        org = build(:organization)
        api_client = APIClient.new('sandbox')

        api_client.create_organization(org)

        expect(fhir_client).to have_received(:create_organization).with(
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

      it 'fails for orgs not in sandbox'
    end

    context 'with a profile endpoint persisted' do
      it 'sends full org data to API'
    end
  end
end