# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do
  include DpcClientSupport
  describe '#create_public_key' do
    before(:each) do
      @public_key_params = { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read, snippet_signature: 'stubbed_sign_txt_signature' }
    end

    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          registered_org = build(:registered_organization)

          api_client = stub_api_client(message: :create_public_key, success: true, response: { id: 'some-id' })

          manager = PublicKeyManager.new(registered_organization: registered_org)

          new_public_key = manager.create_public_key(**@public_key_params)

          expect(api_client).to have_received(:create_public_key)
            .with(registered_org.api_id, params: @public_key_params)
          expect(new_public_key[:response]).to eq(true)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          registered_org = build(:registered_organization)

          api_client = stub_api_client(message: :create_public_key, success: false, response: { id: 'none' })

          manager = PublicKeyManager.new(registered_organization: registered_org)

          new_public_key = manager.create_public_key(**@public_key_params)

          expect(api_client).to have_received(:create_public_key)
            .with(registered_org.api_id, params: @public_key_params)
          expect(new_public_key[:response]).to eq(false)
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        registered_org = build(:registered_organization)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('private_key.pem').read, snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
      end

      it 'returns false when key is not in pem format' do
        registered_org = build(:registered_organization)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read, snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
      end
    end
  end

  describe '#public_keys' do
    context 'successful API request' do
      it 'returns array of public keys' do
        registered_org = build(:registered_organization)

        api_client = stub_api_client(message: :get_public_keys, success: true,
                                     response: { 'entities' => ['id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3'] })

        manager = PublicKeyManager.new(registered_organization: registered_org)
        expect(manager.public_keys).to eq(['id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3'])
        expect(api_client).to have_received(:get_public_keys)
          .with(registered_org.api_id)
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        registered_org = build(:registered_organization)

        api_client = stub_api_client(message: :get_public_keys, success: false, response: { error: 'Bad request' })

        manager = PublicKeyManager.new(registered_organization: registered_org)
        expect(manager.public_keys).to eq([])
        expect(api_client).to have_received(:get_public_keys)
          .with(registered_org.api_id)
      end
    end
  end
end
