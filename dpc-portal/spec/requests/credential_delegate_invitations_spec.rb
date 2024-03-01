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
      expect(assigns(:organization).dpc_api_organization_id).to eq api_id
      expect(response).to have_http_status(200)
    end

    it 'creates ProviderOrganization with org data if not exists' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      expect do
        get "/organizations/#{api_id}/credential_delegate_invitations/new"
      end.to change { ProviderOrganization.count }.by(1)
      expect(assigns(:organization).name).to eq "Bob's Health Hut"
      expect(assigns(:organization).npi).to eq '1111111111'
    end

    it 'uses ProviderOrganization if exists' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      create(:provider_organization, dpc_api_organization_id: api_id, name: 'Foo', npi: '2222222222')
      expect do
        get "/organizations/#{api_id}/credential_delegate_invitations/new"
      end.to change { ProviderOrganization.count }.by(0)
      expect(assigns(:organization).name).to eq 'Foo'
      expect(assigns(:organization).npi).to eq '2222222222'
    end
  end

  describe 'POST /create' do
    let!(:user) { create(:user) }
    let!(:api_id) { SecureRandom.uuid }
    let!(:successful_parameters) do
      { invited_given_name: 'Bob',
        invited_family_name: 'Hodges',
        phone_raw: '222-222-2222',
        invited_email: 'bob@example.com',
        invited_email_confirmation: 'bob@example.com' }
    end

    before do
      sign_in user
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
    end

    it 'creates invitation record on success' do
      expect do
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      end.to change { Invitation.count }.by(1)
    end

    it 'redirects on success' do
      post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      expect(response).to redirect_to(success_organization_credential_delegate_invitation_path(api_id,
                                                                                               'new-invitation'))
    end

    it 'does not create invitation record on failure' do
      successful_parameters['invited_given_name'] = ''
      expect do
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      end.to change { Invitation.count }.by(0)
    end

    it 'does not redirect on failure' do
      successful_parameters['invited_given_name'] = ''
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
      expect(assigns(:organization).dpc_api_organization_id).to eq api_id
      expect(response).to have_http_status(200)
    end
  end
end
