# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'IpAddresses', type: :request do
  include LoginSupport

  before(:example) { WebMock.disable_net_connect!(allow_localhost: true, allow: ['api']) }
  after(:example) { WebMock.disable_net_connect!(allow_localhost: true) }

  describe 'IP Addresses', :integration do
    let(:dpc_api_organization_id) { SecureRandom.uuid }
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:link) { create(:cd_org_link, user:, provider_organization: org) }
    let(:ipv4_address) { '136.226.19.87' }
    before do
      org.update!(terms_of_service_accepted_by: user)
      sign_in user
    end
    it 'should create an ip address, show on org page, and delete it' do
      get "/organizations/#{org.id}"
      expect(response.body).to include('You have no public IP addresses.')

      post "/organizations/#{org.id}/ip_addresses", params: { ip_address: ipv4_address }
      expect(response).to redirect_to(organization_path(org, credential_start: true))
      expect(assigns(:organization)).to eq org
      expect(flash[:success]).to eq('Public IP address created successfully.')

      get "/organizations/#{org.id}"
      expect(response.body).to_not include('You have no public IP addresses.')
      expect(response.body).to include(ipv4_address)

      delete_path_match = %r{action="(/organizations/#{org.id}/ip_addresses/[^"]+)}.match(response.body)
      expect(delete_path_match).to be_truthy

      delete delete_path_match[1]

      expect(flash[:success]).to eq('Public IP address deleted successfully.')
      expect(response).to redirect_to(organization_path(org, credential_start: true))

      get "/organizations/#{org.id}"
      expect(response.body).to include('You have no public IP addresses.')
    end
  end
end
