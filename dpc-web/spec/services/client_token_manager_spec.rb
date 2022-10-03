# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ClientTokenManager do
  describe '#create_client_token' do
    context 'successful API request' do
      it 'responds true with @client_token set' do
        registered_org = build(:registered_organization)
        token = { 'token' => 'exampleToken' }
        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:create_client_token)
          .with(registered_org.api_id, params: { label: 'Test Token 1' })
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return(token)

        manager = ClientTokenManager.new(registered_organization: registered_org)
        expect(manager.create_client_token(label: 'Test Token 1')).to eq(true)
        expect(manager.client_token).to eq(token)
      end
    end

    context 'failed API request' do
      it 'responds false' do
        registered_org = build(:registered_organization)
        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:create_client_token)
          .with(registered_org.api_id, params: { label: 'Test Token 1' })
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(false)
        allow(api_client).to receive(:response_body).and_return(nil)

        manager = ClientTokenManager.new(registered_organization: registered_org)
        expect(manager.create_client_token(label: 'Test Token 1')).to eq(false)
        expect(manager.client_token).to eq(nil)
      end
    end
  end

  describe '#client_tokens' do
    context 'successful API request' do
      it 'responds with client_tokens array' do
        registered_org = build(:registered_organization)
        tokens = [{ 'token' => 'exampleToken' }]
        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:get_client_tokens)
          .with(registered_org.api_id)
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return('entities' => tokens)

        manager = ClientTokenManager.new(registered_organization: registered_org)
        expect(manager.client_tokens).to eq(tokens)
      end
    end

    context 'failed API request' do
      it 'responds with empty array' do
        registered_org = build(:registered_organization)
        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:get_client_tokens)
          .with(registered_org.api_id)
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(false)
        allow(api_client).to receive(:response_body).and_return(error: 'Bad credentials')

        manager = ClientTokenManager.new(registered_organization: registered_org)
        expect(manager.client_tokens).to eq([])
      end
    end
  end
end
