# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do
  before(:each) do
    @public_key_params = { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read,
                           snippet_signature: 'stubbed_sign_txt_signature' }
  end

  describe '#create_public_key' do
    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          registered_org = build(:registered_organisation)

          response = { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' }
          stub_api_client(message: :create_public_key, success: true, response:)

          manager = PublicKeyManager.new(registered_organization: registered_org)
          new_public_key = manager.create_public_key(**@public_key_params)

          expect(new_public_key[:response]).to eq(true)
          expect(new_public_key[:message]).to eq(response)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          registered_org = build(:registered_organization)

          response = { 'id' => nil }
          stub_api_client(message: :create_public_key, success: false, response:)

          manager = PublicKeyManager.new(registered_organization: registered_org)
          new_public_key = manager.create_public_key(**@public_key_params)

          expect(new_public_key[:response]).to eq(false)
          expect(new_public_key[:message]).to eq(response)
          expect(new_public_key[:errors]).to eq(root: PublicKeyManager::SERVER_ERROR_MSG)
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        registered_org = build(:registered_organization)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('private_key.pem').read,
                                                   snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
        expect(new_public_key[:errors]).to eq(public_key: 'Must be a public key (not a private key).',
                                              root: PublicKeyManager::INVALID_KEY)
      end

      it 'returns false when key is not in pem format' do
        registered_org = build(:registered_organization)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read,
                                                   snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
        expect(new_public_key[:errors]).to eq(public_key: 'Must be a valid public key.',
                                              root: PublicKeyManager::INVALID_KEY)
      end

      it 'return false when key is duplicate' do
        registered_org = build(:registered_organisation)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        response = 'duplicate key value violates unique constraint'
        stub_api_client(message: :create_public_key, success: false, response:)

        duplicate_key = manager.create_public_key(**@public_key_params)

        expect(duplicate_key[:response]).to eq(false)
        expect(duplicate_key[:message]).to eq(I18n.t('errors.duplicate_key.text'))
      end
    end
  end

  describe '#delete_public_key' do
    context 'with valid key' do
      let(:key_guid) { SecureRandom.uuid }
      context 'successful API request' do
        it 'responds true' do
          stub_api_client(message: :delete_public_key, success: true)

          response = manager.delete_public_key(id: key_guid)

          expect(response).to be true
        end
      end

      context 'failed API request' do
        it 'responds false' do
          stub_api_client(message: :delete_public_key, success: false)

          response = manager.delete_public_key(id: key_guid)

          expect(response).to be false
        end
      end
    end
  end

  describe '#public_keys' do
    context 'successful API request' do
      it 'returns array of public keys' do
        keys = [{ 'id' => SecureRandom.uuid }]
        stub_api_client(message: :get_public_keys, success: true, response: { 'entities' => keys })

        registered_org = build(:registered_organization)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        expect(manager.public_keys).to eq(keys)
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        response = { error: 'Bad request' }
        stub_api_client(message: :get_public_keys, success: false, response:)

        registered_org = build(:registered_organization)
        manager = PublicKeyManager.new(registered_organization: registered_org)

        expect(manager.public_keys).to eq([])
        expect(manager.errors).to eq(response)
      end
    end
  end
end
