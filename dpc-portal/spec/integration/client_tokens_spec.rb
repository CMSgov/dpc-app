# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'ClientTokens', type: :request do
  include LoginSupport

  before(:example) { WebMock.disable_net_connect!(allow_localhost: true, allow: ['api']) }
  after(:example) { WebMock.disable_net_connect!(allow_localhost: true) }

  describe 'Client Tokens', :integration do
    let(:dpc_api_organization_id) { SecureRandom.uuid }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let(:label) { 'New Client Token' }

    CspUtils::CODES_TO_DISPLAY.each do |provider, display_name|
      context "using #{display_name}" do
        before do
          user = create_user_with_csp(csp: provider)
          create(:cd_org_link, user:, provider_organization: org)
          org.update!(terms_of_service_accepted_by: user)
          sign_in user, csp: provider
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

        it 'redirects with alert when id contains path traversal' do
          delete "/organizations/#{org.id}/client_tokens/../../etc/passwd"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'redirects with alert when id contains special characters' do
          delete "/organizations/#{org.id}/client_tokens/<script>alert(1)</script>"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'redirects with alert when id is blank' do
          delete "/organizations/#{org.id}/client_tokens/%20"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'redirects with alert when id exceeds 64 characters' do
          long_id = 'a' * 65
          delete "/organizations/#{org.id}/client_tokens/#{long_id}"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'does not call the API when id is invalid' do
          expect_any_instance_of(ClientTokenManager).not_to receive(:delete_client_token)
          delete "/organizations/#{org.id}/client_tokens/../../etc/passwd"
        end
      end
    end
  end
end
