# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do
  let(:api_id) { SecureRandom.uuid }
  let(:manager) { PublicKeyManager.new(api_id) }
  before(:each) do
    @public_key_params = { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read,
                           snippet_signature: 'stubbed_sign_txt_signature' }
  end

  describe '#create_public_key' do
    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          response = { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' }
          stub_self_returning_api_client(message: :create_public_key,
                                         response:,
                                         with: [api_id, { params: @public_key_params }])

          new_public_key = manager.create_public_key(**@public_key_params)

          expect(new_public_key[:response]).to eq(true)
          expect(new_public_key[:message]).to eq(response)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          response = { 'id' => nil }
          stub_self_returning_api_client(message: :create_public_key,
                                         success: false,
                                         response:,
                                         with: [api_id, { params: @public_key_params }])

          new_public_key = manager.create_public_key(**@public_key_params)

          expect(new_public_key[:response]).to eq(false)
          expect(new_public_key[:message]).to eq(response)
          expect(new_public_key[:errors]).to eq({ root: PublicKeyManager::SERVER_ERROR_MSG })

        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('private_key.pem').read,
                                                   snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
        expect(new_public_key[:errors]).to eq(public_key: 'Must be a public key (not a private key).',
                                              root: PublicKeyManager::INVALID_KEY)
      end

      it 'returns false when key is not in pem format' do
        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read,
                                                   snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
        expect(new_public_key[:errors]).to eq(public_key: 'Must be a valid public key.',
                                              root: PublicKeyManager::INVALID_KEY)
      end

      it 'return false when key is duplicate' do
        response = { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' }
        stub_self_returning_api_client(message: :create_public_key,
                                       response: ,
                                       with: [api_id, { params: @public_key_params }])

        new_public_key = manager.create_public_key(**@public_key_params)
        expect(new_public_key[:response]).to eq(true)

        duplicate_key = manager.create_public_key(**@public_key_params)
        expect(duplicate_key[:response]).to eq(false)
        expect(duplicate_key[:message]).to eq(I18n.t('errors.duplicate_key.text'))
      end
    end
  end

  describe '#delete_public_key' do
    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          allow(@api_client).to receive(:delete_public_key)
            .with(@registered_org.api_id, '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3')
            .and_return(true)
          allow(@api_client).to receive(:create_public_key).and_return(@api_client)
          allow(@api_client).to receive(:response_successful?).and_return(true)
          allow(@api_client).to receive(:response_body).and_return('id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3')

          manager.create_public_key(**@public_key_params)
          new_public_key = manager.delete_public_key({ id: '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' })

          expect(new_public_key).to eq(true)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          allow(@api_client).to receive(:delete_public_key)
            .with(@registered_org.api_id, '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3')
            .and_return(false)
          allow(@api_client).to receive(:create_public_key)
            .with(@registered_org.api_id, params: @public_key_params)
            .and_return(@api_client)
          allow(@api_client).to receive(:response_body).and_return('id' => 'none')
          allow(@api_client).to receive(:response_successful?).and_return(false)

          manager.create_public_key(**@public_key_params)
          new_public_key = manager.delete_public_key({ id: '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' })

          expect(new_public_key).to eq(false)
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('private_key.pem').read, snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
      end

      it 'returns false when key is not in pem format' do
        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read, snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
      end
    end
  end

  describe '#public_keys' do
    context 'successful API request' do
      it 'returns array of public keys' do
        allow(@api_client).to receive(:get_public_keys)
          .with(@registered_org.api_id).and_return(@api_client)
        allow(@api_client).to receive(:response_successful?).and_return(true)
        allow(@api_client).to receive(:response_body).and_return('entities' => ['id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3'])

        expect(manager.public_keys).to eq(['id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3'])
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        allow(@api_client).to receive(:get_public_keys)
          .with(@registered_org.api_id).and_return(@api_client)
        allow(@api_client).to receive(:response_successful?).and_return(false)
        allow(@api_client).to receive(:response_body).and_return('error' => 'Bad request')

        expect(manager.public_keys).to eq([])
      end
    end
  end
end
