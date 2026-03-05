# frozen_string_literal: true

require 'base64'
require 'openssl'
require 'rails_helper'

RSpec.describe DpcClient, type: :integration do
  let(:client) { DpcClient.new }
  let(:npi) { '1111111112' }
  let!(:org) do
    double('organization',
           npi:, name: 'Org 2', address_use: 'work', address_type: 'both',
           address_city: 'Akron', address_state: 'OH', address_street: '111 Main ST', 'address_street_2' => 'STE 5',
           id: '8453e48b-0b42-4ddf-8b43-07c7aa2a3d88',
           address_zip: '22222')
  end
  let!(:reg_org) do
    double('RegisteredOrg', api_id: 'some-api-key')
  end

  before(:all) do
    WebMock.disable!
  end
  after(:all) do
    WebMock.enable!
  end

  describe '#create_organization' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        client.create_organization(org)

        expect(client.response_status).to eq(200)
        expect(client.response_body.dig('identifier', 0, 'value')).to eq npi
      end
    end
  end

  describe '#get_organization_by_npi' do
    context 'successful API request' do
      it 'retrieves organization data from API' do
        response = client.get_organization_by_npi(npi)
        expect(response&.entry).to_not be_nil
        expect(response.entry.length).to eq 1
        expect(response.entry.first.resource.identifier.first.value).to eq npi
      end
    end
  end

  describe '#post_create' do
    let(:org_id) do
      response = client.get_organization_by_npi(npi)
      response.entry.first.resource.id
    end

    describe '#get_organization' do
      context 'successful API request' do
        it 'retrieves organization data from API' do
          response = client.get_organization(org_id)
          expect(response).to_not be_nil
          expect(response.resourceType).to eq 'Organization'
        end
      end
    end

    describe '#update_organization' do
      context 'successful request' do
        it 'sends org data to API' do
          expect(client.update_organization(org, org_id)).to eq(client)
          expect(client.response_successful?).to eq(true)
        end
      end
    end

    describe '#create_client_token' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.create_client_token(org_id, params: { label: 'Sandbox Token 1' })

          expect(client.response_status).to eq(200)
          expect(client.response_body['label']).to eq('Sandbox Token 1')
        end
      end
    end

    describe '#get_client_tokens' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.get_client_tokens(org_id)

          expect(client.response_status).to eq(200)
          expect(client.response_body['count']).to be > 0
          expect(client.response_body['entities'].first['label']).to eq('Sandbox Token 1')
        end
      end
    end

    describe '#delete_client_token' do
      context 'successful API request' do
        it 'returns success' do
          client.get_client_tokens(org_id)
          expect(client.response_status).to eq(200)
          start_count = client.response_body['count']
          expect(start_count).to be > 0
          token_id = client.response_body['entities'].first['id']
          client.delete_client_token(org_id, token_id)
          expect(client.response_status).to eq(204)
          client.get_client_tokens(org_id)
          expect(client.response_status).to eq(200)
          expect(client.response_body['count'] + 1).to eq start_count
        end
      end
    end

    describe '#create_public_key' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          rsa_key = OpenSSL::PKey::RSA.new(4096)

          rsa_key.to_pem

          public_key = rsa_key.public_key.to_pem

          message = 'This is the snippet used to verify a key pair in DPC.'

          digest = OpenSSL::Digest.new('SHA256')

          signature_binary = rsa_key.sign(digest, message)

          snippet_signature = Base64.encode64(signature_binary)

          label = 'Sandbox Key 1'
          client.create_public_key(
            org_id,
            params: { label:, public_key:, snippet_signature: }
          )

          expect(client.response_status).to eq(200)
          expect(client.response_body['label']).to eq label
        end
      end
    end

    describe '#get_public_keys' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.get_public_keys(org_id)

          expect(client.response_status).to eq(200)
          expect(client.response_body['count']).to be > 0
          expect(client.response_body['entities'].first['label']).to eq('Sandbox Key 1')
        end
      end
    end

    describe '#delete_public_key' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.get_public_keys(org_id)
          expect(client.response_status).to eq(200)
          start_count = client.response_body['count']
          expect(start_count).to be > 0
          public_key_id = client.response_body['entities'].first['id']
          client.delete_public_key(
            org_id,
            public_key_id
          )
          expect(client.response_status).to eq(200)
          client.get_public_keys(org_id)
          expect(client.response_status).to eq(200)
          expect(client.response_body['count'] + 1).to eq start_count
        end
      end
    end

    describe '#create_ip_address' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.create_ip_address(org_id, params: { label: 'Sandbox IP 1', ip_address: '136.226.19.87' })
          expect(client.response_status).to eq(200)
          expect(client.response_body['label']).to eq 'Sandbox IP 1'
        end
      end
    end

    describe '#get_ip_addresses' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.get_ip_addresses(org_id)
          expect(client.response_status).to eq(200)
          expect(client.response_body['count']).to be > 0
          expect(client.response_body['entities'].first['label']).to eq('Sandbox IP 1')
        end
      end
    end

    describe '#delete_ip_address' do
      context 'successful API request' do
        it 'sends data to API and sets response instance variables' do
          client.get_ip_addresses(org_id)
          expect(client.response_status).to eq(200)
          start_count = client.response_body['count']
          expect(start_count).to be > 0
          ip_address_id = client.response_body['entities'].first['id']
          client.delete_ip_address(
            org_id,
            ip_address_id
          )
          expect(client.response_status).to eq(204)
          client.get_ip_addresses(org_id)
          expect(client.response_status).to eq(200)
          expect(client.response_body['count'] + 1).to eq start_count
        end
      end
    end
  end

  def stubbed_key
    file_fixture('stubbed_key.pem').read
  end
end
