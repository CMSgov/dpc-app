# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'PublicKeys', type: :request do
  include DpcClientSupport

  describe 'GET /new' do
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{api_id}/public_keys/new"
      expect(assigns(:organization).api_id).to eq api_id
      expect(response).to have_http_status(200)
    end
  end

  describe 'POST /create' do
    it 'succeeds if label' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_public_key,
                                     response: default_get_public_keys,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/public_keys",
           params: { label: 'New Key', organization_id: org_api_id, public_key: 'test key' }
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(response).to redirect_to(organization_path(org_api_id))
    end

    it 'fails if no label' do
      org_api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(org_api_id))
      post "/organizations/#{org_api_id}/public_keys"
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(flash[:alert]).to eq('Required values missing.')
    end

    it 'fails if label over 25 characters' do
      org_api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(org_api_id))
      post "/organizations/#{org_api_id}/public_keys",
           params: { label: 'aaaaabbbbbcccccdddddeeeeefffff', organization_id: org_api_id, public_key: 'test key' }
      expect(flash[:alert]).to eq('Label cannot be over 25 characters')
    end

    it 'shows error if problem' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_public_key,
                                     success: false,
                                     response: nil,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/public_keys",
           params: { label: 'New Key', organization_id: org_api_id, public_key: 'test key' }
      expect(flash[:alert]).to eq('Public key could not be created.')
    end
  end

  describe 'DELETE /destroy' do
    it 'flashes success if succeeds' do
      org_api_id = SecureRandom.uuid
      key_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_public_key,
                                     response: nil,
                                     with: [org_api_id, key_guid],
                                     api_client: api_client)
      delete "/organizations/#{org_api_id}/public_keys/#{key_guid}"
      expect(flash[:notice]).to eq('Public key successfully deleted.')
      expect(response).to redirect_to(organization_path(org_api_id))
    end
    it 'renders error if error' do
      org_api_id = SecureRandom.uuid
      key_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_client_token,
                                     response: nil,
                                     success: false,
                                     with: [org_api_id, key_guid],
                                     api_client: api_client)
      delete "/organizations/#{org_api_id}/public_keys/#{key_guid}"
      expect(flash[:alert]).to eq('Public key could not be deleted.')
      expect(response).to redirect_to(organization_path(org_api_id))
    end
  end

  describe 'GET #download_snippet' do
    context 'when the snippet is requested' do
      it 'serves the snippet file' do
        stub_api_client(
          message: :get_organization,
          response: default_get_org_response(org_id)
        )
        post :download_snippet, params: {}
        expect(response.status).to eq(202)
        expect(response.header['Content-Type']).to eq('application/zip')
        expect(response.body).to eq('This is the snippet used to verify a key pair in DPC.')
      end
    end
  end
end
