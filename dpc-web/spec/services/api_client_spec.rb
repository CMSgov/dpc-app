# frozen_string_literal: true

require 'rails_helper'

RSpec.describe APIClient do
  describe '#create_organization' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        org = create(:organization)
        create(:fhir_endpoint,
          name: 'Cool SBX',
          uri: 'https://cool.com',
          status: 'active',
          organization: org
        )

        allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
        allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')

        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer 112233' },
          body: {
            resourceType: 'Parameters',
            parameter: [{
              name: 'resource',
              resource: {
                resourceType: 'Bundle',
                type: 'collection',
                entry: [{
                  resource: {
                    address: [{
                      use: org.address_use,
                      type: org.address_type,
                      city: org.address_city,
                      country: 'US',
                      line: [org.address_street, org.address_street_2],
                      postalCode: org.address_zip,
                      state: org.address_state
                    }],
                    identifier: [{system: 'http://hl7.org/fhir/sid/us-npi', value: nil}],
                    name: org.name,
                    resourceType: 'Organization',
                    type: [{
                      coding: [{
                        code: 'prov', display: 'Healthcare Provider', system: 'http://hl7.org/fhir/organization-type'
                      }],
                      text: 'Healthcare Provider'
                    }]
                  }
                }, {
                  resource: {
                    resourceType: 'Endpoint',
                    status: 'active',
                    connectionType: {system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type', code: 'hl7-fhir-rest'},
                    name: 'Cool SBX', address: 'https://cool.com'
                  }
                }]
              }
            }]
          }.to_json
        ).to_return(
          status: 200,
          body: "{\"id\":\"8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d\",\"endpoint\":[{\"reference\":\"Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66\"}]}"
        )

        api_client = APIClient.new('sandbox')

        api_client.create_organization(org)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          {
            'id' => '8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d',
            'endpoint' => [{ 'reference' => 'Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66' }]
          }
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        org = create(:organization)
        create(:fhir_endpoint,
          name: 'Cool SBX',
          uri: 'https://cool.com',
          status: 'active',
          organization: org
        )

        allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
        allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')

        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer 112233' },
          body: {
            resourceType: 'Parameters',
            parameter: [{
              name: 'resource',
              resource: {
                resourceType: 'Bundle',
                type: 'collection',
                entry: [{
                  resource: {
                    address: [{
                      use: org.address_use,
                      type: org.address_type,
                      city: org.address_city,
                      country: 'US',
                      line: [org.address_street, org.address_street_2],
                      postalCode: org.address_zip,
                      state: org.address_state
                    }],
                    identifier: [{system: 'http://hl7.org/fhir/sid/us-npi', value: nil}],
                    name: org.name,
                    resourceType: 'Organization',
                    type: [{
                      coding: [{
                        code: 'prov', display: 'Healthcare Provider', system: 'http://hl7.org/fhir/organization-type'
                      }],
                      text: 'Healthcare Provider'
                    }]
                  }
                }, {
                  resource: {
                    resourceType: 'Endpoint',
                    status: 'active',
                    connectionType: {system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type', code: 'hl7-fhir-rest'},
                    name: 'Cool SBX', address: 'https://cool.com'
                  }
                }]
              }
            }]
          }.to_json
        ).to_return(
          status: 500,
          body: "{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"severity\":\"fatal\",\"details\":{\"text\":\"org.hibernate.exception.ConstraintViolationException: could not execute statement\"}}]}"
        )

        api_client = APIClient.new('sandbox')

        api_client.create_organization(org)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {
            'resourceType'=>'OperationOutcome',
            'issue'=>[{
              'severity'=>'fatal',
              'details'=>{
                'text'=>'org.hibernate.exception.ConstraintViolationException: could not execute statement'
              }
            }]
          }
        )
      end
    end
  end
end
