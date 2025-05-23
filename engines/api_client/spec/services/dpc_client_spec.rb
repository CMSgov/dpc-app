# frozen_string_literal: true

require 'rails_helper'

RSpec.describe DpcClient do
  let!(:org) do
    double('organization',
           npi: '1111111112', name: 'Org', address_use: 'work', address_type: 'both',
           address_city: 'Akron', address_state: 'OH', address_street: '111 Main ST', 'address_street_2' => 'STE 5',
           address_zip: '22222')
  end
  let!(:reg_org) do
    double('RegisteredOrg', api_id: 'some-api-key')
  end

  # rubocop:disable Layout/LineLength
  before(:each) do
    allow(ENV).to receive(:fetch).with('API_METADATA_URL').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('API_ADMIN_URL').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON').and_return('MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo')
    allow(ENV).to receive(:fetch).with('ALLOW_INVALID_SSL_CERT').and_return('false')
  end
  # rubocop:enable Layout/LineLength

  describe '#get_organization' do
    let(:headers) do
      {
        'Accept' => 'application/fhir+json',
        'Accept-Encoding' => 'gzip;q=1.0,deflate;q=0.6,identity;q=0.3',
        'User-Agent' => 'Ruby',
        'Authorization' => /.*/
      }
    end
    context 'successful API request' do
      it 'retrieves organization data from API' do
        body = '{"resourceType":"Organization"}'
        stub_request(:get, "http://dpc.example.com/Organization/#{reg_org.api_id}")
          .with(headers:).to_return(status: 200, body:, headers: {})
        client = DpcClient.new
        response = client.get_organization(reg_org.api_id)
        expect(response).to_not be_nil
        expect(response.resourceType).to eq 'Organization'
      end
    end

    context 'unsuccessful request' do
      it 'does not retrieve organization data from API' do
        stub_request(:get, "http://dpc.example.com/Organization/#{reg_org.api_id}")
          .with(headers:).to_return(status: 500, body: '', headers: {})

        client = DpcClient.new
        response = client.get_organization(reg_org.api_id)
        expect(response).to be_nil
      end

      it 'on credential error' do
        stub_request(:get, "http://dpc.example.com/Organization/#{reg_org.api_id}")
          .with(headers:).to_return(status: 401, body: 'Requires Credentials', headers: {})

        client = DpcClient.new
        response = client.get_organization(reg_org.api_id)
        expect(response).to be_nil
      end
    end
  end

  describe '#get_organization_by_npi' do
    let(:headers) do
      {
        'Accept' => 'application/fhir+json',
        'Accept-Encoding' => 'gzip;q=1.0,deflate;q=0.6,identity;q=0.3',
        'Content-Type' => 'application/fhir+json',
        'User-Agent' => 'Ruby',
        'Authorization' => /.*/
      }
    end
    context 'successful API request' do
      it 'retrieves organization data from API' do
        body = { resourceType: 'Bundle',
                 entry: [{ resource: { resourceType: 'Organization',
                                       id: '6dae2c89-6344-4f62-a334-ec4be642ecb4',
                                       identifier: [{ system: 'http://hl7.org/fhir/sid/us-npi',
                                                      value: '3304239163' }] } }] }.to_json
        stub_request(:get, "http://dpc.example.com/Admin/Organization?npis=npi|#{org.npi}")
          .with(headers:).to_return(status: 200, body:, headers: {})
        client = DpcClient.new
        response = client.get_organization_by_npi(org.npi)
        expect(response&.entry).to_not be_nil
        expect(response.entry.length).to eq 1
        expect(response.entry.first.resource.id).to eq '6dae2c89-6344-4f62-a334-ec4be642ecb4'
      end
    end

    context 'unsuccessful request' do
      it 'does not retrieve organization data from API' do
        stub_request(:get, "http://dpc.example.com/Admin/Organization?npis=npi|#{org.npi}")
          .with(headers:).to_return(status: 500, body: '', headers: {})

        client = DpcClient.new
        response = client.get_organization_by_npi(org.npi)
        expect(response).to be_nil
      end
    end
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
                }]
              }
            }]
          }.to_json
        ).to_return(
          status: 200,
          body: '{"id":"8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d"}'
        )

        api_client = DpcClient.new

        api_client.create_organization(org)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          {
            'id' => '8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d'
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

        api_client = DpcClient.new

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
                }]
              }
            }]
          }.to_json
        ).to_return(
          status: 500,
          body: '{"resourceType":"OperationOutcome","issue":[{"severity":"fatal","details":{' \
                '"text":"org.hibernate.exception.ConstraintViolationException: could not execute statement"}}]}'
        )

        api_client = DpcClient.new

        api_client.create_organization(org)
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
      it 'sends org data to API' do
        stub_request(:put, "http://dpc.example.com/Organization/#{reg_org.api_id}")
          .with(
            body: /#{reg_org.api_id}/,
            headers: {
              'Accept' => 'application/fhir+json',
              'Content-Type' => 'application/fhir+json',
              'Authorization' => /.*/
            }
          ).to_return(status: 200, body: '{}', headers: {})

        client = DpcClient.new
        expect(client.update_organization(org, reg_org.api_id)).to eq(client)
        expect(client.response_successful?).to eq(true)
      end
    end

    context 'unsuccessful request' do
      it 'does not send org data to API' do
        stub_request(:put, "http://dpc.example.com/Organization/#{reg_org.api_id}")
          .with(
            body: /#{reg_org.api_id}/,
            headers: {
              'Accept' => 'application/fhir+json',
              'Content-Type' => 'application/fhir+json',
              'Authorization' => /.*/
            }
          ).to_return(status: 500, body: '', headers: {})

        client = DpcClient.new
        expect(client.update_organization(org, reg_org.api_id)).to eq(client)
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

        api_client = DpcClient.new

        api_client.create_client_token(reg_org.api_id, params: { label: 'Sandbox Token 1' })

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

        api_client = DpcClient.new

        api_client.create_client_token(reg_org.api_id, params: { label: 'Sandbox Token 1' })

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
                '"createdAt":"2019-11-07T17:15:22.781Z","expiresAt":"2019-11-07T17:15:22.781Z"}]'
        )

        api_client = DpcClient.new

        api_client.get_client_tokens(reg_org.api_id)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          [{
            'id' => '4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66',
            'label' => 'Sandbox Token 1',
            'createdAt' => '2019-11-07T17:15:22.781Z',
            'expiresAt' => '2019-11-07T17:15:22.781Z'
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

        api_client = DpcClient.new

        api_client.get_client_tokens(reg_org.api_id)

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

        api_client = DpcClient.new

        api_client.get_client_tokens(reg_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq('')
      end
    end
  end

  describe '#delete_client_token' do
    context 'successful API request' do
      it 'returns success' do
        stub_request(:delete, 'http://dpc.example.com/Token/some-token-id')
          .with(
            headers: { 'Accept' => 'application/json', 'Content-Type' => 'application/json' }
          )
          .to_return(status: 204, headers: {})

        api_client = DpcClient.new

        api_client.delete_client_token(reg_org.api_id, 'some-token-id')
        expect(api_client.response_status).to eq(204)
      end
    end

    context 'unsuccessful API request' do
      it 'returns failure' do
        stub_request(:delete, 'http://dpc.example.com/Token/some-token-id')
          .with(
            headers: { 'Accept' => 'application/json', 'Content-Type' => 'application/json' }
          )
          .to_return(status: 500, body: '', headers: {})

        api_client = DpcClient.new

        api_client.delete_client_token(reg_org.api_id, 'some-token-id')
        expect(api_client.response_status).to eq(500)
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

        api_client = DpcClient.new

        api_client.create_public_key(
          reg_org.api_id,
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

        api_client = DpcClient.new

        api_client.create_public_key(
          reg_org.api_id,
          params: { label: 'Sandbox Key 1', public_key: stubbed_key, snippet_signature: 'signature_snippet' }
        )

        stub_request(:delete, 'http://dpc.example.com/Key/3fa85f64-5717-4562-b3fc-2c963f66afa6')
          .with(
            headers: { 'Accept' => 'application/json', 'Content-Type' => 'application/json' }
          )
          .to_return(status: 200, body: '', headers: {})

        api_client.delete_public_key(
          reg_org.api_id,
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

        api_client = DpcClient.new

        api_client.create_public_key(
          reg_org.api_id,
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

        api_client = DpcClient.new

        api_client.get_public_keys(reg_org.api_id)

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

        api_client = DpcClient.new

        api_client.get_public_keys(reg_org.api_id)

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq('{}')
      end
    end

    describe '#create_ip_address' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          stub_request(:post, 'http://dpc.example.com/IpAddress').with(
            body: { ip_address: '136.226.19.87', label: 'Sandbox IP 1' }
          ).to_return(
            status: 200,
            body: '{"label":"Sandbox IP 1","createdAt":"2019-11-07T19:38:44.205Z",' \
                  '"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}'
          )

          api_client = DpcClient.new
          api_client.create_ip_address(reg_org.api_id, params: { label: 'Sandbox IP 1', ip_address: '136.226.19.87' })
          expect(api_client.response_status).to eq(200)
          expect(api_client.response_body).to eq(
            {
              'label' => 'Sandbox IP 1',
              'createdAt' => '2019-11-07T19:38:44.205Z',
              'id' => '3fa85f64-5717-4562-b3fc-2c963f66afa6'
            }
          )
        end
      end
    end

    describe '#delete_ip_address' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          stub_request(:post, 'http://dpc.example.com/IpAddress').with(
            body: { ip_address: '136.226.19.87', label: 'Sandbox IP 1' }
          ).to_return(
            status: 200,
            body: '{"label":"Sandbox IP 1","createdAt":"2019-11-07T19:38:44.205Z",' \
                  '"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}'
          )

          api_client = DpcClient.new
          api_client.create_ip_address(
            reg_org.api_id,
            params: { label: 'Sandbox IP 1', ip_address: '136.226.19.87' }
          )
          stub_request(:delete, 'http://dpc.example.com/IpAddress/3fa85f64-5717-4562-b3fc-2c963f66afa6')
            .with(
              headers: { 'Accept' => 'application/json', 'Content-Type' => 'application/json' }
            )
            .to_return(status: 200, body: '', headers: {})

          api_client.delete_ip_address(
            reg_org.api_id,
            '3fa85f64-5717-4562-b3fc-2c963f66afa6'
          )
          expect(api_client.response_status).to eq(200)
        end
      end

      context 'unsuccessful API request' do
        it 'sends data to API and sets response instance variables' do
          stub_request(:post, 'http://dpc.example.com/IpAddress').with(
            body: { ip_address: '136.226.19.87', label: 'Sandbox IP 1' }
          ).to_return(
            status: 500,
            body: '{}'
          )

          api_client = DpcClient.new
          api_client.create_ip_address(
            reg_org.api_id,
            params: {
              label: 'Sandbox IP 1',
              ip_address: '136.226.19.87'
            }
          )
          expect(api_client.response_status).to eq(500)
          expect(api_client.response_body).to eq('{}')
        end
      end
    end

    describe '#get_ip_addresses' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          stub_request(:get, 'http://dpc.example.com/Key').with(
            headers: { 'Content-Type' => 'application/json' }
          ).to_return(
            status: 200,
            body: '[{"id":"4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66","label":"Sandbox IP 1",' \
                  '"createdAt":"2019-11-07T17:15:22.781Z"}]'
          )

          api_client = DpcClient.new
          api_client.get_public_keys(reg_org.api_id)
          expect(api_client.response_status).to eq(200)
          expect(api_client.response_body).to eq(
            [{
              'id' => '4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66',
              'label' => 'Sandbox IP 1',
              'createdAt' => '2019-11-07T17:15:22.781Z'
            }]
          )
        end
      end

      context 'unsuccessful API request' do
        it 'sends data to API and sets response instance variables' do
          stub_request(:get, 'http://dpc.example.com/IpAddress').with(
            headers: { 'Content-Type' => 'application/json' }
          ).to_return(
            status: 500,
            body: '{}'
          )

          api_client = DpcClient.new
          api_client.get_ip_addresses(reg_org.api_id)
          expect(api_client.response_status).to eq(500)
          expect(api_client.response_body).to eq('{}')
        end
      end
    end
  end

  describe '#get healthcheck' do
    context 'successful api request' do
      it 'calls healthcheck' do
        stub_request(:get, 'http://dpc.example.com/healthcheck')
          .to_return(
            status: 200,
            body: ''
          )

        api_client = DpcClient.new
        api_client.healthcheck
        expect(api_client.response_status).to eq(200)
      end
    end

    context 'unsuccessful api request' do
      it 'calls healthcheck and gets bad response' do
        stub_request(:get, 'http://dpc.example.com/healthcheck').with(
          headers: {
            'Content-Type' => 'application/json',
            'Accept' => 'application/json'
          }
        ).to_return(
          status: 500,
          body: ''
        )

        api_client = DpcClient.new
        api_client.healthcheck
        expect(api_client.response_status).to eq(500)
      end
      it 'cannot reach healthcheck due to error' do
        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        allow(http_stub).to receive(:request).and_raise(Socket::ResolutionError)

        api_client = DpcClient.new
        api_client.healthcheck
        expect(api_client.response_status).to eq(500)
      end
    end
  end

  describe 'check ssl settings' do
    before do
      # Force healthcheck to go over https for these tests
      allow(ENV).to receive(:fetch).with('API_ADMIN_URL').and_return('https://dpc.example.com')
      allow(Rails.env).to receive(:development?).and_return(false)
      allow(Rails.env).to receive(:test?).and_return(false)
    end

    context 'not ignoring ssl errors' do
      it 'sets open ssl verify mode to peer' do
        stub_request(:get, 'https://dpc.example.com/healthcheck')
          .with(
            headers: {
              'Accept' => 'application/json',
              'Accept-Encoding' => 'gzip;q=1.0,deflate;q=0.6,identity;q=0.3',
              'Content-Type' => 'application/json',
              'User-Agent' => 'Ruby'
            }
          )
          .to_return(status: 200, body: '', headers: {})

        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        # Let the request error out, we only care about the SSL settings
        allow(http_stub).to receive(:request).and_raise(Socket::ResolutionError)
        expect(http_stub).to receive(:use_ssl=).with(true)
        expect(http_stub).to receive(:verify_mode=).with(OpenSSL::SSL::VERIFY_PEER)

        api_client = DpcClient.new
        api_client.healthcheck
      end
    end

    context 'ignoring ssl errors' do
      it 'sets open ssl verify mode to none' do
        allow(ENV).to receive(:fetch).with('ALLOW_INVALID_SSL_CERT').and_return('true')

        stub_request(:get, 'https://dpc.example.com/healthcheck')
          .with(
            headers: {
              'Accept' => 'application/json',
              'Accept-Encoding' => 'gzip;q=1.0,deflate;q=0.6,identity;q=0.3',
              'Content-Type' => 'application/json',
              'User-Agent' => 'Ruby'
            }
          )
          .to_return(status: 200, body: '', headers: {})

        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        # Let the request error out, we only care about the SSL settings
        allow(http_stub).to receive(:request).and_raise(Socket::ResolutionError)
        expect(http_stub).to receive(:use_ssl=).with(true)
        expect(http_stub).to receive(:verify_mode=).with(OpenSSL::SSL::VERIFY_NONE)

        api_client = DpcClient.new
        api_client.healthcheck
      end
    end
  end

  def stubbed_key
    file_fixture('stubbed_key.pem').read
  end
end
