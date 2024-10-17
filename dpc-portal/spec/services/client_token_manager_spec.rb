# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ClientTokenManager do
  include DpcClientSupport
  let(:api_id) { SecureRandom.uuid }
  let(:manager) { ClientTokenManager.new(api_id) }
  describe '#create_client_token' do
    context 'successful API request' do
      it 'responds true with @client_token set' do
        token = 'exampleToken'
        label = { params: { label: 'Test Token 1' } }
        stub_self_returning_api_client(message: :create_client_token,
                                       response: token,
                                       with: [api_id, label])
        new_token = manager.create_client_token(label: 'Test Token 1')

        expect(new_token[:response]).to eq(true)
        expect(new_token[:message]).to eq(token)
      end
    end

    context 'failed API request' do
      it 'responds false' do
        label = { params: { label: 'Test Token 1' } }
        stub_self_returning_api_client(message: :create_client_token,
                                       success: false,
                                       response: nil,
                                       with: [api_id, label])
        new_token = manager.create_client_token(label: 'Test Token 1')

        expect(new_token[:response]).to eq(false)
        expect(new_token[:errors]).to eq(root: 'Unable to process request')
      end
    end

    context 'invalid values' do
      it 'should fail if label blank' do
        new_token = manager.create_client_token(label: '')
        expect(new_token[:response]).to eq(false)
        expect(new_token[:errors]).to eq(label: 'Cannot be blank')
      end
      it 'should fail if label too long' do
        new_token = manager.create_client_token(label: '12345678901234567890123456')
        expect(new_token[:response]).to eq(false)
        expect(new_token[:errors]).to eq(label: 'Label must be 25 characters or fewer')
      end
    end
  end

  describe '#client_tokens' do
    context 'successful API request' do
      it 'responds with client_tokens array' do
        tokens = [{ 'token' => 'exampleToken' }]
        stub_self_returning_api_client(message: :get_client_tokens,
                                       response: { 'entities' => tokens },
                                       with: [api_id])
        expect(manager.client_tokens).to eq(tokens)
      end
    end

    context 'failed API request' do
      it 'responds with empty array' do
        tokens = [{ 'token' => 'exampleToken' }]
        stub_self_returning_api_client(message: :get_client_tokens,
                                       success: false,
                                       response: { 'entities' => tokens },
                                       with: [api_id])
        expect(manager.client_tokens).to eq([])
      end
    end
  end

  describe '#delete_client_token' do
    it 'responds with success on success' do
      params = { id: 'token-guid' }
      stub_self_returning_api_client(message: :delete_client_token,
                                     with: [api_id, params[:id]])

      expect(manager.delete_client_token(params)).to be true
    end
    it 'responds with failure on failure' do
      params = { id: 'token-guid' }
      stub_self_returning_api_client(message: :delete_client_token,
                                     success: false,
                                     with: [api_id, params[:id]])

      expect(manager.delete_client_token(params)).to be false
    end
  end
end
