# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do
  describe '#create_public_key' do
    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          registered_org = build(:registered_organization, api_env: 'sandbox')

          api_client = instance_double(APIClient)
          allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
          allow(api_client).to receive(:create_public_key)
            .with(registered_org.api_id, params: { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read })
            .and_return(api_client)
          allow(api_client).to receive(:response_successful?).and_return(true)
          allow(api_client).to receive(:response_body).and_return('id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3')

          manager = PublicKeyManager.new(api_env: 'sandbox', registered_organization: registered_org)
          expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read)).to eq(true)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          registered_org = build(:registered_organization, api_env: 'sandbox')

          api_client = instance_double(APIClient)
          allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
          allow(api_client).to receive(:create_public_key)
            .with(registered_org.api_id, params: { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read })
            .and_return(api_client)
          allow(api_client).to receive(:response_successful?).and_return(false)

          manager = PublicKeyManager.new(api_env: 'sandbox', registered_organization: registered_org)
          expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read)).to eq(false)
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        registered_org = build(:registered_organization, api_env: 'sandbox')
        manager = PublicKeyManager.new(api_env: 'sandbox', registered_organization: registered_org)

        expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('private_key.pem').read)).to eq(false)
      end

      it 'returns false when key is not in pem format' do
        registered_org = build(:registered_organization, api_env: 'sandbox')
        manager = PublicKeyManager.new(api_env: 'sandbox', registered_organization: registered_org)

        expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read)).to eq(false)
      end
    end
  end

  describe '#public_keys' do
    context 'successful API request' do
      it 'returns array of public keys' do
        registered_org = build(:registered_organization, api_env: 'sandbox')

        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
        allow(api_client).to receive(:get_public_keys)
          .with(registered_org.api_id).and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return('entities' => ['id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3'])

        manager = PublicKeyManager.new(api_env: 'sandbox', registered_organization: registered_org)
        expect(manager.public_keys).to eq(['id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3'])
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        registered_org = build(:registered_organization, api_env: 'sandbox')

        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
        allow(api_client).to receive(:get_public_keys)
          .with(registered_org.api_id).and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(false)
        allow(api_client).to receive(:response_body).and_return(error: 'Bad request')

        manager = PublicKeyManager.new(api_env: 'sandbox', registered_organization: registered_org)
        expect(manager.public_keys).to eq([])
      end
    end
  end
end
