# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ClientTokenManager do
  include DpcClientSupport
  describe '#create_client_token' do
    context 'successful API request' do
      it 'responds true with @client_token set' do
        api_id = SecureRandom.uuid
        token = 'exampleToken'
        label = { params: { label: 'Test Token 1' } }
        stub_self_returning_api_client(message: :create_client_token,
                                       response: token,
                                       with: [api_id, label])
        manager = ClientTokenManager.new(api_id)
        expect(manager.create_client_token(label: 'Test Token 1')).to eq(true)
        expect(manager.client_token).to eq(token)
      end
    end

    context 'failed API request' do
      it 'responds false' do
        api_id = SecureRandom.uuid
        instance_double(DpcClient)
        label = { params: { label: 'Test Token 1' } }
        stub_self_returning_api_client(message: :create_client_token,
                                       success: false,
                                       response: nil,
                                       with: [api_id, label])
        manager = ClientTokenManager.new(api_id)
        expect(manager.create_client_token(label: 'Test Token 1')).to eq(false)
        expect(manager.client_token).to eq(nil)
      end
    end
  end

  describe '#client_tokens' do
    context 'successful API request' do
      it 'responds with client_tokens array' do
        api_id = SecureRandom.uuid
        tokens = [{ 'token' => 'exampleToken' }]
        stub_self_returning_api_client(message: :get_client_tokens,
                                       response: { 'entities' => tokens },
                                       with: [api_id])
        manager = ClientTokenManager.new(api_id)
        expect(manager.client_tokens).to eq(tokens)
      end
    end

    context 'failed API request' do
      it 'responds with empty array' do
        api_id = SecureRandom.uuid
        tokens = [{ 'token' => 'exampleToken' }]
        stub_self_returning_api_client(message: :get_client_tokens,
                                       success: false,
                                       response: { 'entities' => tokens },
                                       with: [api_id])
        manager = ClientTokenManager.new(api_id)
        expect(manager.client_tokens).to eq([])
      end
    end
  end

  describe '#delete_client_token' do
    it 'responds with success on success' do
      api_id = SecureRandom.uuid
      params = { id: 'token-guid' }
      stub_self_returning_api_client(message: :delete_client_token,
                                     with: [api_id, params[:id]])

      manager = ClientTokenManager.new(api_id)
      expect(manager.delete_client_token(params)).to be true
    end
    it 'responds with failure on failure' do
      api_id = SecureRandom.uuid
      params = { id: 'token-guid' }
      stub_self_returning_api_client(message: :delete_client_token,
                                     success: false,
                                     with: [api_id, params[:id]])

      manager = ClientTokenManager.new(api_id)
      expect(manager.delete_client_token(params)).to be false
    end
  end
end
