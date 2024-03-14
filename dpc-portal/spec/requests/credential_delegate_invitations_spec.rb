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

  describe 'GET /new no link to org' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization) }
    before { sign_in user }
    it 'redirects to organizations' do
      get "/organizations/#{org.id}/credential_delegate_invitations/new"
      expect(response).to redirect_to('/organizations')
    end
  end

  describe 'GET /new no ao link to org' do
    let!(:user) { create(:user) }
    let(:api_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id: api_id) }

    before do
      create(:cd_org_link, provider_organization: org, user:)
      sign_in user
    end

    it 'redirects to organizations' do
      get "/organizations/#{org.id}/credential_delegate_invitations/new"
      expect(response).to redirect_to('/organizations')
    end
  end

  describe 'GET /new' do
    let!(:user) { create(:user) }
    let(:api_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id: api_id) }

    before do
      create(:ao_org_link, provider_organization: org, user:)
      sign_in user
    end

    it 'returns success' do
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{org.id}/credential_delegate_invitations/new"
      expect(assigns(:organization)).to eq org
      expect(response).to have_http_status(200)
    end
  end

  describe 'POST /create' do
    let!(:user) { create(:user) }
    let!(:api_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id: api_id) }
    let!(:successful_parameters) do
      { invited_given_name: 'Bob',
        invited_family_name: 'Hodges',
        phone_raw: '222-222-2222',
        invited_email: 'bob@example.com',
        invited_email_confirmation: 'bob@example.com' }
    end

    before do
      sign_in user
      create(:ao_org_link, provider_organization: org, user:)
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
    end

    it 'creates invitation record on success' do
      expect do
        post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
      end.to change { Invitation.count }.by(1)
    end

    it 'adds verification code to invitation record on success' do
      post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
      expect(assigns(:cd_invitation).verification_code.length).to eq 6
    end

    it 'redirects on success' do
      post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
      expect(response).to redirect_to(success_organization_credential_delegate_invitation_path(org,
                                                                                               'new-invitation'))
    end

    it 'does not create invitation record on failure' do
      successful_parameters['invited_given_name'] = ''
      expect do
        post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
      end.to change { Invitation.count }.by(0)
    end

    it 'does not redirect on failure' do
      successful_parameters['invited_given_name'] = ''
      post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
      expect(response.status).to eq(400)
    end
  end

  describe 'GET /success' do
    let!(:user) { create(:user) }
    let!(:api_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id: api_id) }

    before do
      sign_in user
      create(:ao_org_link, provider_organization: org, user:)
    end
    it 'returns success' do
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{org.id}/credential_delegate_invitations/foo/success"
      expect(assigns(:organization).dpc_api_organization_id).to eq api_id
      expect(response).to have_http_status(200)
    end
  end
end
