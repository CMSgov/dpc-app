# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'CredentialDelegateInvitations', type: :request do
  include DpcClientSupport

  describe 'GET /new not logged in' do
    it 'redirects to login' do
      get '/organizations/no-such-id/credential_delegate_invitations/new'
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'GET /new' do
    let!(:user) { create(:user) }

    before { sign_in user }
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{api_id}/credential_delegate_invitations/new"
      expect(assigns(:organization).api_id).to eq api_id
      expect(response).to have_http_status(200)
    end
  end

  describe 'POST /create' do
    let!(:user) { create(:user) }
    let!(:api_id) { SecureRandom.uuid }
    let!(:successful_parameters) do
      { given_name: 'Bob',
        family_name: 'Hodges',
        phone_raw: '222-222-2222',
        email: 'bob@example.com',
        email_confirmation: 'bob@example.com' }
    end

    before do
      sign_in user
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
    end
    it 'redirects on success' do
      post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      expect(response).to redirect_to(success_organization_credential_delegate_invitation_path(api_id,
                                                                                               'new-invitation'))
    end

    it 'does not redirect on failure' do
      successful_parameters['given_name'] = ''
      post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      expect(response.status).to eq(400)
    end
  end

  describe 'GET /success' do
    let!(:user) { create(:user) }

    before { sign_in user }
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{api_id}/credential_delegate_invitations/foo/success"
      expect(assigns(:organization).api_id).to eq api_id
      expect(response).to have_http_status(200)
    end
  end
end
