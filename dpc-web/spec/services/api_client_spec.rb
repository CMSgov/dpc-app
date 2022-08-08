# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.describe APIClient do
  include OrganizationsHelper

  let!(:org) { create(:organization, npi: LuhnacyLib.generate_npi) }
  let!(:registered_org) { build(:registered_organization, organization: org) }
  let!(:fhir_endpoint) do
    build(
      :fhir_endpoint,
      name: 'Cool SBX',
      uri: 'https://cool.com',
      status: 'active',
      registered_organization: registered_org
    )
  end

  before(:each) do
    allow(ENV).to receive(:fetch).with('API_METADATA_URL').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON').and_return('MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo')
  end

  describe '#create_organization' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
          headers: {
            'Content-Type' => 'application/fhir+json',
            'Authorization' => 'Bearer MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjM' \
                               'i1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMT' \
                               'QIW9ACQcZPuhAGxwwo'
          },
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
                    identifier: [{ system: 'http://hl7.org/fhir/sid/us-npi', value: org.npi }],
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
                    connectionType: {
                      system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type',
                      code: 'hl7-fhir-rest'
                    },
                    payloadType: [
                      {
                        'coding': [
                          {
                            'system': 'http://hl7.org/fhir/endpoint-payload-type',
                            'code': 'any'
                          }
                        ]
                      }
                    ],
                    name: fhir_endpoint.name, address: fhir_endpoint.uri
                  }
                }]
              }
            }]
          }.to_json
        ).to_return(
          status: 200,
          body: '{"id":"8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d",' \
                '"endpoint":[{"reference":"Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66"}]}'
        )

        api_client = APIClient.new

        api_client.create_organization(org, fhir_endpoint: fhir_endpoint.attributes)

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
        allow(http_stub).to receive(:use_ssl=).with(false).and_return(false)
        allow(http_stub).to receive(:request).and_raise(Errno::ECONNREFUSED)

        api_client = APIClient.new

        api_client.create_organization(org, fhir_endpoint: fhir_endpoint.attributes)

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
          headers: {
            'Content-Type' => 'application/fhir+json',
            'Authorization' => 'Bearer MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjM' \
                               'i1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMT' \
                               'QIW9ACQcZPuhAGxwwo'
          },
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
                    identifier: [{ system: 'http://hl7.org/fhir/sid/us-npi', value: org.npi }],
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
                    connectionType: {
                      system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type',
                      code: 'hl7-fhir-rest'
                    },
                    payloadType: [
                      {
                        'coding': [
                          {
                            'system': 'http://hl7.org/fhir/endpoint-payload-type',
                            'code': 'any'
                          }
                        ]
                      }
                    ],
                    name: fhir_endpoint.name, address: fhir_endpoint.uri
                  }
                }]
              }
            }]
          }.to_json
        ).to_return(
          status: 500,
          body: '{"resourceType":"OperationOutcome","issue":[{"severity":"fatal","details":{' \
                '"text":"org.hibernate.exception.ConstraintViolationException: could not execute statement"}}]}'
        )

        api_client = APIClient.new

        api_client.create_organization(org, fhir_endpoint: fhir_endpoint.attributes)
        parse_response = JSON.parse api_client.response_body

        expect(api_client.response_status).to eq(500)
        expect(parse_response).to eq(
          {
            'resourceType' => 'OperationOutcome',
            'issue' => [{
              'severity' => 'fatal',
              'details' => {
                'text' => 'org.hibernate.exception.ConstraintViolationException: could not execute statement'
              }
            }]
          }
        )
      end
    end
  end

  describe '#update_organization' do
    context 'successful request' do
      it 'uses fhir_client to send org data to API' do
        stub_request(:put, "http://dpc.example.com/Organization/#{registered_org.api_id}")
          .with(
            body: /#{registered_org.api_id}/,
            headers: {
              'Accept' => 'application/fhir+json',
              'Content-Type' => 'application/fhir+json;charset=utf-8',
              'Authorization' => /.*/
            }
          ).to_return(status: 200, body: '{}', headers: {})

        client = APIClient.new
        expect(client.update_organization(registered_org)).to eq(client)
        expect(client.response_successful?).to eq(true)
      end
    end

    context 'unsuccessful request' do
      it 'uses fhir_client to send org data to API' do
        stub_request(:put, "http://dpc.example.com/Organization/#{registered_org.api_id}")
          .with(
            body: /#{registered_org.api_id}/,
            headers: {
              'Accept' => 'application/fhir+json',
              'Content-Type' => 'application/fhir+json;charset=utf-8',
              'Authorization' => /.*/
            }
          ).to_return(status: 500, body: '', headers: {})

        client = APIClient.new
        expect(client.update_organization(registered_org)).to eq(client)
        expect(client.response_successful?).to eq(false)
      end
    end
  end

  describe '#update_endpoint' do
    context 'successful request' do
      it 'uses fhir_client to send endpoint data to API' do
        build(:fhir_endpoint, registered_organization: registered_org)

        stub_request(:put, "http://dpc.example.com/Endpoint/#{registered_org.fhir_endpoint_id}")
          .with(
            body: /#{registered_org.fhir_endpoint_id}/,
            headers: {
              'Accept' => 'application/fhir+json',
              'Content-Type' => 'application/fhir+json;charset=utf-8',
              'Authorization' => /.*/
            }
          ).to_return(status: 200, body: '{}', headers: {})

        client = APIClient.new
        expect(client.update_endpoint(registered_org)).to eq(client)
        expect(client.response_successful?).to eq(true)
      end
    end

    context 'unsuccessul request' do
      it 'uses fhir_client to send org data to API' do
        build(:fhir_endpoint, registered_organization: registered_org)

        stub_request(:put, "http://dpc.example.com/Endpoint/#{registered_org.fhir_endpoint_id}")
          .with(
            body: /#{registered_org.fhir_endpoint_id}/,
            headers: {
              'Accept' => 'application/fhir+json',
              'Content-Type' => 'application/fhir+json;charset=utf-8',
              'Authorization' => /.*/
            }
          ).to_return(status: 500, body: '', headers: {})

        client = APIClient.new
        expect(client.update_endpoint(registered_org)).to eq(client)
        expect(client.response_successful?).to eq(false)
      end
    end
  end

  describe '#create_client_token' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Token').with(
          headers: { 'Content-Type' => 'application/json' },
          body: {
            label: 'Sandbox Token 1'
          }.to_json
        ).to_return(
          status: 200,
          body: '{"token":"1234567890","label":"Sandbox Token 1","createdAt":"2019-11-07T17:15:22.781Z"}'
        )

        api_client = APIClient.new

        api_client.create_client_token(registered_org.api_id, params: { label: 'Sandbox Token 1' })

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          { 'token' => '1234567890', 'label' => 'Sandbox Token 1', 'createdAt' => '2019-11-07T17:15:22.781Z' }
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Token').with(
          headers: { 'Content-Type' => 'application/json' },
          body: {
            label: 'Sandbox Token 1'
          }.to_json
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new

        api_client.create_client_token(registered_org.api_id, params: { label: 'Sandbox Token 1' })

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq('{}')
      end
    end
  end

  describe '#get_client_tokens' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, 'http://dpc.example.com/Token').with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 200,
          body: '[{"id":"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66","label":"Sandbox Token 1",' \
                '"createdAt":"2019-11-07T17:15:22.781Z","expiresdAt":"2019-11-07T17:15:22.781Z"}]'
        )

        api_client = APIClient.new

        api_client.get_client_tokens(registered_org.api_id)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          [{
            'id' => '4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66',
            'label' => 'Sandbox Token 1',
            'createdAt' => '2019-11-07T17:15:22.781Z',
            'expiresdAt' =>'2019-11-07T17:15:22.781Z'
          }]
        )
      end
    end

    context 'unsuccessful API request' do
      it 'responds like 500 if connection error is raised' do
        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        allow(http_stub).to receive(:use_ssl=).with(false).and_return(false)
        allow(http_stub).to receive(:request).and_raise(Errno::ECONNREFUSED)

        api_client = APIClient.new

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
        stub_request(:get, 'http://dpc.example.com/Token').with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 500,
          body: ''
        )

        api_client = APIClient.new

        api_client.get_client_tokens(registered_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq('')
      end
    end
  end

  describe '#create_public_key' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Key?label=Sandbox+Key+1').with(
          body: {
            key: stubbed_key,
            signature: 'signature_snippet'
          }
        ).to_return(
          status: 200,
          body: '{"label":"Sandbox Key 1","createdAt":"2019-11-07T19:38:44.205Z",' \
                '"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}'
        )

        api_client = APIClient.new

        api_client.create_public_key(
          registered_org.api_id,
          params: { label: 'Sandbox Key 1', public_key: stubbed_key, snippet_signature: 'signature_snippet' }
        )

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          {
            'label' => 'Sandbox Key 1',
            'createdAt' => '2019-11-07T19:38:44.205Z',
            'id' => '3fa85f64-5717-4562-b3fc-2c963f66afa6'
          }
        )
      end
    end
  end

  describe '#delete_public_key' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Key?label=Sandbox+Key+1').with(
          body: {
            key: stubbed_key,
            signature: 'signature_snippet'
          }
        ).to_return(
          status: 200,
          body: '{"label":"Sandbox Key 1","createdAt":"2019-11-07T19:38:44.205Z",' \
                '"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}'
        )

        api_client = APIClient.new

        api_client.create_public_key(
          registered_org.api_id,
          params: { label: 'Sandbox Key 1', public_key: stubbed_key, snippet_signature: 'signature_snippet' }
        )

        stub_request(:delete, "http://dpc.example.com/Key/3fa85f64-5717-4562-b3fc-2c963f66afa6").
         with(
           headers: {
       	  'Accept'=>'application/json',
       	  'Content-Type'=>'application/json',
           }).
         to_return(status: 200, body: "", headers: {})

        api_client.delete_public_key(
          registered_org.api_id,
          '3fa85f64-5717-4562-b3fc-2c963f66afa6'
        )

        expect(api_client.response_status).to eq(200)
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, 'http://dpc.example.com/Key?label=Sandbox+Key+1').with(
          body: {
            key: stubbed_key,
            signature: 'stubbed_sign_txt_signature'
          }
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new

        api_client.create_public_key(
          registered_org.api_id,
          params: {
            label: 'Sandbox Key 1',
            public_key: stubbed_key,
            snippet_signature: 'stubbed_sign_txt_signature'
          }
        )

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq('{}')
      end
    end
  end

  describe '#get_public_keys' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, 'http://dpc.example.com/Key').with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 200,
          body: '[{"id":"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66","label":"Sandbox Key 1",' \
                '"createdAt":"2019-11-07T17:15:22.781Z"}]'
        )

        api_client = APIClient.new

        api_client.get_public_keys(registered_org.api_id)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          [{
            'id' => '4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66',
            'label' => 'Sandbox Key 1',
            'createdAt' => '2019-11-07T17:15:22.781Z'
          }]
        )
      end
    end

    context 'unsuccessful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:get, 'http://dpc.example.com/Key').with(
          headers: { 'Content-Type' => 'application/json' }
        ).to_return(
          status: 500,
          body: '{}'
        )

        api_client = APIClient.new

        api_client.get_public_keys(registered_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq('{}')
      end
    end
  end

  def stubbed_key
    file_fixture('stubbed_key.pem').read
  end
end
