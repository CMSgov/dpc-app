# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'ClientTokens', type: :request do
  before(:example) { WebMock.disable_net_connect!(allow_localhost: true, allow: ['api']) }
  after(:example) { WebMock.disable_net_connect!(allow_localhost: true) }

  describe 'Client Tokens', :integration do
    let(:dpc_api_organization_id) { SecureRandom.uuid }
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:link) { create(:cd_org_link, user:, provider_organization: org) }
    let(:label) { 'New Client Token' }
    before do
      org.update!(terms_of_service_accepted_by: user)
      sign_in user
    end
    it 'should generate a client token, show on org page, and delete it' do
      get "/organizations/#{org.id}"
      expect(response.body).to include('You have no client tokens.')
      expect(response.body).to_not include(label)

      post "/organizations/#{org.id}/client_tokens", params: { label: }
      expect(response).to be_ok
      expect(assigns(:organization)).to eq org
      expect(flash[:success]).to eq('Client token created successfully.')

      get "/organizations/#{org.id}"
      expect(response.body).to_not include('You have no client tokens.')
      expect(response.body).to include(label)

      delete_path_match = %r{action="(/organizations/#{org.id}/client_tokens/[^"]+)}.match(response.body)
      expect(delete_path_match).to be_truthy

      delete delete_path_match[1]

      expect(flash[:success]).to eq('Client token deleted successfully.')
      expect(response).to redirect_to(organization_path(org, credential_start: true))

      get "/organizations/#{org.id}"
      expect(response.body).to include('You have no client tokens.')
      expect(response.body).to_not include(label)
    end
  end
end
