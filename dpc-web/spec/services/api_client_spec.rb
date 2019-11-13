# frozen_string_literal: true

require 'rails_helper'

RSpec.describe APIClient do
  let!(:org) { create(:organization, npi: 'cool-npi-1') }
  let!(:registered_org) { create(:registered_organization, organization: org) }
  let!(:fhir_endpoint) { create(:fhir_endpoint, name: 'Cool SBX', uri: 'https://cool.com',
                                                status: 'active', organization: org) }

  before(:each) do
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('MDAxY2xvY2F0aW9uIGh0dHA6Ly9teWJhbmsvCjAwMjZpZGVudGlmaWVyIHdlIHVzZWQgb3VyIHNlY3JldCBrZXkKMDAxNmNpZCB0ZXN0ID0gY2F2ZWF0CjAwMmZzaWduYXR1cmUgGXusegRK8zMyhluSZuJtSTvdZopmDkTYjOGpmMI9vWcK')
  end

  describe '#create_organization' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer MDAxY2xvY2F0aW9uIGh0dHA6Ly9teWJhbmsvCjAwMjZpZGVudGlmaWVyIHdlIHVzZWQgb3VyIHNlY3JldCBrZXkKMDAxNmNpZCB0ZXN0ID0gY2F2ZWF0CjAwMmZzaWduYXR1cmUgGXusegRK8zMyhluSZuJtSTvdZopmDkTYjOGpmMI9vWcK' },
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
                    identifier: [{system: 'http://hl7.org/fhir/sid/us-npi', value: 'cool-npi-1'}],
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
                    status: fhir_endpoint.status,
                    connectionType: {system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type', code: 'hl7-fhir-rest'},
                    name: fhir_endpoint.name, address: fhir_endpoint.uri
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
        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer MDAxY2xvY2F0aW9uIGh0dHA6Ly9teWJhbmsvCjAwMjZpZGVudGlmaWVyIHdlIHVzZWQgb3VyIHNlY3JldCBrZXkKMDAxNmNpZCB0ZXN0ID0gY2F2ZWF0CjAwMmZzaWduYXR1cmUgGXusegRK8zMyhluSZuJtSTvdZopmDkTYjOGpmMI9vWcK' },
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
                    identifier: [{system: 'http://hl7.org/fhir/sid/us-npi', value: 'cool-npi-1'}],
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
                    status: fhir_endpoint.status,
                    connectionType: {system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type', code: 'hl7-fhir-rest'},
                    name: fhir_endpoint.name, address: fhir_endpoint.uri
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

  describe '#create_client_token' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, "http://dpc.example.com/Token").with(
          headers: { 'Content-Type' => 'application/json' },
          body: {
            label: 'Sandbox Token 1'
          }.to_json
        ).to_return(
          status: 200,
          body: "{\"token\":\"1234567890\",\"label\":\"Sandbox Token 1\",\"createdAt\":\"2019-11-07T17:15:22.781Z\"}"
        )

        api_client = APIClient.new('sandbox')

        api_client.create_client_token(registered_org.api_id, params: { label: 'Sandbox Token 1' })

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          { 'token' => '1234567890', 'label' => 'Sandbox Token 1', 'createdAt' => '2019-11-07T17:15:22.781Z' }
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, "http://dpc.example.com/Token").with(
          headers: { 'Content-Type' => 'application/json' },
          body: {
            label: 'Sandbox Token 1'
          }.to_json
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new('sandbox')

        api_client.create_client_token(registered_org.api_id, params: { label: 'Sandbox Token 1' })

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {}
        )
      end
    end
  end

  describe '#get_client_tokens' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, "http://dpc.example.com/Token").with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 200,
          body: "[{\"id\":\"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66\",\"label\":\"Sandbox Token 1\",\"createdAt\":\"2019-11-07T17:15:22.781Z\",\"expiresdAt\":\"2019-11-07T17:15:22.781Z\"}]"
        )

        api_client = APIClient.new('sandbox')

        api_client.get_client_tokens(registered_org.api_id)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          [{
            "id"=>"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66",
            "label"=>"Sandbox Token 1",
            "createdAt"=>"2019-11-07T17:15:22.781Z",
            "expiresdAt"=>"2019-11-07T17:15:22.781Z"
          }]
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, "http://dpc.example.com/Token").with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new('sandbox')

        api_client.get_client_tokens(registered_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {}
        )
      end
    end
  end
end
