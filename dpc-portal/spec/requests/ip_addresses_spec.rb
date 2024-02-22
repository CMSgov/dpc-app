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

  describe 'GET /new' do
    let!(:user) { create(:user) }
    before { sign_in user }

    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{api_id}/ip_addresses/new"
      expect(assigns(:organization).api_id).to eq api_id
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
    before { sign_in user }

    it 'succeeds with valid params' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     response: default_get_ip_addresses['entities'].first,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/ip_addresses", params: { label: 'Public IP 1', ip_address: '136.226.19.87' }
      expect(assigns(:organization).api_id).to eq org_api_id
    end

    it 'fails if missing params' do
      org_api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(org_api_id))
      post "/organizations/#{org_api_id}/ip_addresses"
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(flash[:alert]).to eq('IP address could not be created: missing label, missing IP address.')
    end

    it 'fails if invalid IP' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     response: default_get_ip_addresses['entities'].first,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/ip_addresses", params: { label: 'Public IP 1', ip_address: '333.333.333.333' }
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(flash[:alert]).to eq('IP address could not be created: invalid IP address.')
    end

    it 'fails if label over 25 characters' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     response: default_get_ip_addresses['entities'].first,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/ip_addresses",
           params: { label: 'aaaaabbbbbcccccdddddeeeeefffff', ip_address: '136.226.19.87' }
      expect(assigns(:organization).api_id).to eq org_api_id
      expect(flash[:alert]).to eq('IP address could not be created: label cannot be over 25 characters.')
    end

    it 'shows error if problem' do
      org_api_id = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :create_ip_address,
                                     success: false,
                                     response: nil,
                                     api_client: api_client)
      post "/organizations/#{org_api_id}/ip_addresses", params: { label: 'Public IP 1', ip_address: '136.226.19.87' }
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
    before { sign_in user }

    it 'flashes success if succeeds' do
      org_api_id = SecureRandom.uuid
      addr_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_ip_address,
                                     response: nil,
                                     with: [org_api_id, addr_guid],
                                     api_client: api_client)
      delete "/organizations/#{org_api_id}/ip_addresses/#{addr_guid}"
      expect(flash[:notice]).to eq('IP address successfully deleted.')
      expect(response).to redirect_to(organization_path(org_api_id))
    end

    it 'renders error if error' do
      org_api_id = SecureRandom.uuid
      addr_guid = SecureRandom.uuid
      api_client = stub_api_client(message: :get_organization,
                                   response: default_get_org_response(org_api_id))
      stub_self_returning_api_client(message: :delete_ip_address,
                                     response: nil,
                                     success: false,
                                     with: [org_api_id, addr_guid],
                                     api_client: api_client)
      delete "/organizations/#{org_api_id}/ip_addresses/#{addr_guid}"
      expect(flash[:alert]).to eq('IP address could not be deleted: failed to delete IP address.')
      expect(response).to redirect_to(organization_path(org_api_id))
    end
  end
end
