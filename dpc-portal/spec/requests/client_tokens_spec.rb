# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'ClientTokens', type: :request do
  include DpcClientSupport

  describe 'GET /new' do
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{api_id}/client_tokens/new"
      expect(assigns(:organization).api_id).to eq api_id
      expect(response).to have_http_status(200)
    end
  end

  describe 'POST /create' do
    it 'succeeds if label' do
      org_api_id = SecureRandom.uuid
      token_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_client_token,
                                     response: default_get_client_tokens(guid: token_guid)['entities'].first,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/client_tokens", params: { label: 'New Token' }
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(assigns(:client_token)['id']).to eq token_guid
    end

    it 'fails if no label' do
      org_api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(org_api_id))
      post "/organizations/#{org_api_id}/client_tokens"
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(flash[:alert]).to eq('Label required.')
    end

    it 'shows error if problem' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_client_token,
                                     success: false,
                                     response: nil,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/client_tokens", params: { label: 'New Token' }
      expect(flash[:alert]).to eq('Client token could not be created.')
    end
  end

  describe 'DELETE /destroy' do
    it 'flashes success if succeeds' do
      org_api_id = SecureRandom.uuid
      token_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_client_token,
                                     response: nil,
                                     with: [org_api_id, token_guid],
                                     api_client: api_client)
      delete "/organizations/#{org_api_id}/client_tokens/#{token_guid}"
      expect(flash[:notice]).to eq('Client token successfully deleted.')
      expect(response).to redirect_to(organization_path(org_api_id))
    end
    it 'renders error if error' do
      org_api_id = SecureRandom.uuid
      token_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_client_token,
                                     response: nil,
                                     success: false,
                                     with: [org_api_id, token_guid],
                                     api_client: api_client)
      delete "/organizations/#{org_api_id}/client_tokens/#{token_guid}"
      expect(flash[:alert]).to eq('Client token could not be deleted.')
      expect(response).to redirect_to(organization_path(org_api_id))
    end
  end
end
