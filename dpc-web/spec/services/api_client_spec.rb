# frozen_string_literal: true

require 'rails_helper'

RSpec.describe APIClient do
  let!(:org) { create(:organization, npi: 'cool-npi-1') }
  let!(:registered_org) { create(:registered_organization, organization: org) }
  let!(:fhir_endpoint) { create(:fhir_endpoint, name: 'Cool SBX', uri: 'https://cool.com',
                                                status: 'active', organization: org) }

  before(:each) do
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo')
  end

  describe '#create_organization' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo' },
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
      it 'responds like 500 if connection error is raised' do
        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        allow(http_stub).to receive(:request).and_raise(Errno::ECONNREFUSED)

        api_client = APIClient.new('sandbox')

        api_client.create_organization(org)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {
            'issue' => [{
              'details' => {
                'text' => 'Connection error'
              }
            }]
          }
        )
      end

      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo' },
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
      it 'responds like 500 if connection error is raised' do
        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        allow(http_stub).to receive(:request).and_raise(Errno::ECONNREFUSED)

        api_client = APIClient.new('sandbox')

        api_client.get_client_tokens(registered_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {
            'issue' => [{
              'details' => {
                'text' => 'Connection error'
              }
            }]
          }
        )
      end

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

  describe '#create_public_key' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, "http://dpc.example.com/Key").with(
          body: { label: 'Sandbox Key 1', key: file_fixture('stubbed_key.pem').read }.to_json
        ).to_return(
          status: 200,
          body: "{\"label\":\"Sandbox Key 1\",\"createdAt\":\"2019-11-07T19:38:44.205Z\",\"id\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"}"
        )

        api_client = APIClient.new('sandbox')

        api_client.create_public_key(registered_org.api_id, params: { label: 'Sandbox Key 1', key: file_fixture('stubbed_key.pem').read })

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          { 'label'=>'Sandbox Key 1', 'createdAt'=>'2019-11-07T19:38:44.205Z', 'id'=>'3fa85f64-5717-4562-b3fc-2c963f66afa6' }
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, "http://dpc.example.com/Key").with(
          body: { label: 'Sandbox Key 1', key: file_fixture('stubbed_key.pem').read }.to_json
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new('sandbox')

        api_client.create_public_key(registered_org.api_id, params: { label: 'Sandbox Key 1', key: file_fixture('stubbed_key.pem').read })

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {}
        )
      end
    end
  end

  describe '#get_public_keys' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, "http://dpc.example.com/Key").with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 200,
          body: "[{\"id\":\"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66\",\"label\":\"Sandbox Key 1\",\"createdAt\":\"2019-11-07T17:15:22.781Z\"}]"
        )

        api_client = APIClient.new('sandbox')

        api_client.get_public_keys(registered_org.api_id)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          [{
            "id"=>"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66",
            "label"=>"Sandbox Key 1",
            "createdAt"=>"2019-11-07T17:15:22.781Z"
          }]
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, "http://dpc.example.com/Key").with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new('sandbox')

        api_client.get_public_keys(registered_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {}
        )
      end
    end
  end
end
