# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'ClientTokens', type: :request do
  include DpcClientSupport

  describe 'GET /new' do
    context 'not logged in' do
      it 'redirects to login' do
        get '/organizations/no-such-id/client_tokens/new'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'no link to org' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }
      before { sign_in user }
      it 'redirects to organizations' do
        get "/organizations/#{org.id}/client_tokens/new"
        expect(response).to redirect_to('/organizations')
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'returns success' do
        get "/organizations/#{org.id}/client_tokens/new"
        expect(assigns(:organization)).to eq org
        expect(response).to have_http_status(200)
      end
    end
  end

  describe 'Post /create' do
    context 'not logged in' do
      it 'redirects to login' do
        post '/organizations/no-such-id/client_tokens'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let(:org_api_id) { SecureRandom.uuid }
      let!(:org) { create(:provider_organization, dpc_api_organization_id: org_api_id) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'succeeds if label' do
        token_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :create_client_token,
                                       response: default_get_client_tokens(guid: token_guid)['entities'].first,
                                       api_client:)
        post "/organizations/#{org.id}/client_tokens", params: { label: 'New Token' }
        expect(assigns(:organization)).to eq org
        expect(assigns(:client_token)['id']).to eq token_guid
      end

      it 'fails if no label' do
        post "/organizations/#{org.id}/client_tokens"
        expect(assigns(:organization)).to eq org
        expect(flash[:alert]).to eq('Label required.')
      end

      it 'shows error if problem' do
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :create_client_token,
                                       success: false,
                                       response: nil,
                                       api_client:)
        post "/organizations/#{org.id}/client_tokens", params: { label: 'New Token' }
        expect(flash[:alert]).to eq('Client token could not be created.')
      end
    end
  end

  describe 'Delete /destroy' do
    context 'not logged in' do
      it 'redirects to login' do
        delete '/organizations/no-such-id/client_tokens/no-such-id'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let(:org_api_id) { SecureRandom.uuid }
      let!(:org) { create(:provider_organization, dpc_api_organization_id: org_api_id) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'flashes success if succeeds' do
        token_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :delete_client_token,
                                       response: nil,
                                       with: [org_api_id, token_guid],
                                       api_client:)
        delete "/organizations/#{org.id}/client_tokens/#{token_guid}"
        expect(flash[:notice]).to eq('Client token successfully deleted.')
        expect(response).to redirect_to(organization_path(org.id))
      end

      it 'renders error if error' do
        token_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :delete_client_token,
                                       response: nil,
                                       success: false,
                                       with: [org_api_id, token_guid],
                                       api_client:)
        delete "/organizations/#{org.id}/client_tokens/#{token_guid}"
        expect(flash[:alert]).to eq('Client token could not be deleted.')
        expect(response).to redirect_to(organization_path(org.id))
      end
    end
  end
end
