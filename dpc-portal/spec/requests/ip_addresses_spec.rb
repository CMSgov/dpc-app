# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'IpAddresses', type: :request do
  include DpcClientSupport

  describe 'GET /new not logged in' do
    it 'redirects to login' do
      get '/organizations/no-such-id/ip_addresses/new'
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'GET /new no link to org' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization) }
    before { sign_in user }
    it 'redirects to organizations' do
      get "/organizations/#{org.id}/client_tokens/new"
      expect(response).to redirect_to('/organizations')
    end
  end

  describe 'GET /new' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization) }

    before do
      create(:cd_org_link, provider_organization: org, user:)
      sign_in user
    end

    it 'returns success' do
      get "/organizations/#{org.id}/ip_addresses/new"
      expect(assigns(:organization)).to eq org
      expect(response).to have_http_status(200)
    end
  end

  describe 'Post /create not logged in' do
    it 'redirects to login' do
      post '/organizations/no-such-id/ip_addresses'
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'POST /create' do
    let!(:user) { create(:user) }
    let(:org_api_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id: org_api_id) }

    before do
      create(:cd_org_link, provider_organization: org, user:)
      sign_in user
    end

    it 'succeeds with valid params' do
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     response: default_get_ip_addresses['entities'].first,
                                     api_client:)
      post "/organizations/#{org.id}/ip_addresses", params: { label: 'Public IP 1', ip_address: '136.226.19.87' }
      expect(assigns(:organization)).to eq org
    end

    it 'fails if missing params' do
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(org_api_id))
      post "/organizations/#{org.id}/ip_addresses"
      expect(assigns(:organization)).to eq org
      expect(flash[:alert]).to eq('IP address could not be created: missing label, missing IP address.')
    end

    it 'fails if invalid IP' do
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     response: default_get_ip_addresses['entities'].first,
                                     api_client:)
      post "/organizations/#{org.id}/ip_addresses", params: { label: 'Public IP 1', ip_address: '333.333.333.333' }
      expect(assigns(:organization)).to eq org
      expect(flash[:alert]).to eq('IP address could not be created: invalid IP address.')
    end

    it 'fails if label over 25 characters' do
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     response: default_get_ip_addresses['entities'].first,
                                     api_client:)
      post "/organizations/#{org.id}/ip_addresses",
           params: { label: 'aaaaabbbbbcccccdddddeeeeefffff', ip_address: '136.226.19.87' }
      expect(assigns(:organization)).to eq org
      expect(flash[:alert]).to eq('IP address could not be created: label cannot be over 25 characters.')
    end

    it 'shows error if problem' do
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     success: false,
                                     response: nil,
                                     api_client:)
      post "/organizations/#{org.id}/ip_addresses", params: { label: 'Public IP 1', ip_address: '136.226.19.87' }
      expect(flash[:alert]).to eq('IP address could not be created: failed to create IP address.')
    end
  end

  describe 'Delete /destroy not logged in' do
    it 'redirects to login' do
      delete '/organizations/no-such-id/ip_addresses/no-such-id'
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'DELETE /destroy' do
    let!(:user) { create(:user) }
    let(:org_api_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id: org_api_id) }

    before do
      create(:cd_org_link, provider_organization: org, user:)
      sign_in user
    end

    it 'flashes success if succeeds' do
      addr_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_ip_address,
                                     response: nil,
                                     with: [org_api_id, addr_guid],
                                     api_client:)
      delete "/organizations/#{org.id}/ip_addresses/#{addr_guid}"
      expect(flash[:notice]).to eq('IP address successfully deleted.')
      expect(response).to redirect_to(organization_path(org))
    end

    it 'renders error if error' do
      addr_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_ip_address,
                                     response: nil,
                                     success: false,
                                     with: [org_api_id, addr_guid],
                                     api_client:)
      delete "/organizations/#{org.id}/ip_addresses/#{addr_guid}"
      expect(flash[:alert]).to eq('IP address could not be deleted: failed to delete IP address.')
      expect(response).to redirect_to(organization_path(org))
    end
  end
end
