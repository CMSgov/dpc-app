# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do
  include DpcClientSupport

  describe '#create_public_key' do
    before(:each) do
      @public_key_params = { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read,
                             snippet_signature: 'stubbed_sign_txt_signature' }
    end

    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          api_id = SecureRandom.uuid
          response = { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' }
          stub_self_returning_api_client(message: :create_public_key,
                                         response:,
                                         with: [api_id, { params: @public_key_params }])

          manager = PublicKeyManager.new(api_id)

          new_public_key = manager.create_public_key(**@public_key_params)

          expect(new_public_key[:response]).to eq(true)
          expect(new_public_key[:message]).to eq(response)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          api_id = SecureRandom.uuid
          response = { 'id' => nil }
          stub_self_returning_api_client(message: :create_public_key,
                                         success: false,
                                         response:,
                                         with: [api_id, { params: @public_key_params }])

          manager = PublicKeyManager.new(api_id)

          new_public_key = manager.create_public_key(**@public_key_params)

          expect(new_public_key[:response]).to eq(false)
          expect(new_public_key[:message]).to eq(response)
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        api_id = SecureRandom.uuid
        manager = PublicKeyManager.new(api_id)

        force_key_private
        response = manager.create_public_key(label: 'Test Key 1',
                                             public_key: file_fixture('stubbed_key.pem').read,
                                             snippet_signature: 'stubbed_sign_txt_signature')

        expect(response[:response]).to eq(false)
        expect(manager.errors.size).to eq 1
        expect(manager.errors.first).to eq 'Must be a public key'
      end

      it 'returns false when key is not in pem format' do
        api_id = SecureRandom.uuid
        manager = PublicKeyManager.new(api_id)

        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read,
                                                   snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
      end
    end
  end

  describe '#delete_public_key' do
    before(:each) do
      @public_key_params = { label: 'Test Key 1', public_key: file_fixture('stubbed_key.pem').read,
                             snippet_signature: 'stubbed_sign_txt_signature' }
    end

    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          api_id = SecureRandom.uuid
          key_guid = SecureRandom.uuid
          stub_self_returning_api_client(message: :delete_public_key,
                                         with: [api_id, key_guid])

          manager = PublicKeyManager.new(api_id)
          response = manager.delete_public_key(id: key_guid)

          expect(response).to be true
        end
      end

      context 'failed API request' do
        it 'responds false' do
          api_id = SecureRandom.uuid
          key_guid = SecureRandom.uuid
          stub_self_returning_api_client(message: :delete_public_key,
                                         success: false,
                                         with: [api_id, key_guid])

          manager = PublicKeyManager.new(api_id)
          response = manager.delete_public_key(id: key_guid)

          expect(response).to be false
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        api_id = SecureRandom.uuid
        manager = PublicKeyManager.new(api_id)

        force_key_private
        response = manager.create_public_key(label: 'Test Key 1',
                                             public_key: file_fixture('stubbed_key.pem').read,
                                             snippet_signature: 'stubbed_sign_txt_signature')

        expect(response[:response]).to eq(false)
        expect(manager.errors.size).to eq 1
        expect(manager.errors.first).to eq 'Must be a public key'
      end

      it 'returns false when key is not in pem format' do
        api_id = SecureRandom.uuid
        manager = PublicKeyManager.new(api_id)

        new_public_key = manager.create_public_key(label: 'Test Key 1', public_key: file_fixture('bad_cert.pub').read,
                                                   snippet_signature: 'stubbed_sign_txt_signature')

        expect(new_public_key[:response]).to eq(false)
      end
    end
  end

  describe '#public_keys' do
    context 'successful API request' do
      it 'returns array of public keys' do
        api_id = SecureRandom.uuid

        keys = [{ 'id' => SecureRandom.uuid }]
        stub_self_returning_api_client(message: :get_public_keys,
                                       response: { 'entities' => keys },
                                       with: [api_id])

        manager = PublicKeyManager.new(api_id)
        expect(manager.public_keys).to eq(keys)
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        api_id = SecureRandom.uuid

        stub_self_returning_api_client(message: :get_public_keys,
                                       success: false,
                                       response: { error: 'Bad request' },
                                       with: [api_id])

        manager = PublicKeyManager.new(api_id)
        expect(manager.public_keys).to eq([])
        expect(manager.errors).to eq([{ error: 'Bad request' }])
      end
    end
  end

  def force_key_private
    doubled_rsa = instance_double(OpenSSL::PKey::RSA)
    allow(OpenSSL::PKey::RSA).to receive(:new).and_return(doubled_rsa)
    allow(doubled_rsa).to receive(:private?).and_return(true)
  end
end
