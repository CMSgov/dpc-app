# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'
require 'support/credential_resource_shared_examples'
require 'support/user_access_shared_examples'

RSpec.describe 'ClientTokens', type: :request do
  include DpcClientSupport
  include LoginSupport

  let(:terms_of_service_accepted_by) { create(:user) }

  it_behaves_like 'a credential resource' do
    let(:create_params) { { label: 'New Token' } }
    let(:credential) { 'client_token' }
  end

  LoginSupport::CSP_MAP.each do |provider, display_name|
    context "using #{display_name}" do
      describe 'GET /new' do
        new_path = ->(org) { "/organizations/#{org.id}/client_tokens/new" }
        context 'not logged in' do
          it 'redirects to login' do
            get '/organizations/no-such-id/client_tokens/new'
            expect(response).to redirect_to('/users/sign_in')
          end
        end

        context 'ao access denied' do
          context 'user has sanctions' do
            it_behaves_like 'ao access denied with user sanctions', provider, new_path
          end
          context 'org has sanctions' do
            it_behaves_like 'ao access denied with org sanctions', provider, new_path
          end
          context 'org not approved' do
            it_behaves_like 'ao access denied with org not approved', provider, new_path
          end
          context 'user no longer ao' do
            it_behaves_like 'ao access denied user no longer ao', provider, new_path
          end
        end

        context 'cd access denied' do
          context 'org has sanctions' do
            it_behaves_like 'cd access denied with org sanctions', provider, new_path
          end
          context 'org not approved' do
            it_behaves_like 'cd access denied with org not approved', provider, new_path
          end
        end

        context 'no link to org' do
          it_behaves_like 'GET /new with no link to org', provider, new_path
        end
        context 'not signed tos' do
          it_behaves_like 'GET /new with unsigned tos', provider, new_path
        end
        context 'as cd' do
          it_behaves_like 'GET /new as cd returns success', provider, new_path
        end
      end

      describe 'POST /create' do
        context 'not logged in' do
          it 'redirects to login' do
            post '/organizations/no-such-id/client_tokens'
            expect(response).to redirect_to('/users/sign_in')
          end
        end

        context 'as cd' do
          let!(:user)      { create_user_with_csp(provider) }
          let(:org_api_id) { SecureRandom.uuid }
          let!(:org) do
            create(:provider_organization, terms_of_service_accepted_by:,
                                           dpc_api_organization_id: org_api_id)
          end

          before do
            create(:cd_org_link, provider_organization: org, user:)
            sign_in user, csp: provider
          end

          it 'succeeds if label' do
            token_guid = SecureRandom.uuid
            stub_self_returning_api_client(message: :create_client_token,
                                           response: default_get_client_tokens(guid: token_guid)['entities'].first)
            post "/organizations/#{org.id}/client_tokens", params: { label: 'New Token' }
            expect(assigns(:organization)).to eq org
            expect(assigns(:client_token)['id']).to eq token_guid
            expect(flash[:success]).to eq('Client token created successfully.')
          end

          it 'checks if configuration complete on success' do
            config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
            expect(config_complete_checker).to receive(:perform_later).with(org.id)
            stub_self_returning_api_client(message: :create_client_token,
                                           response: default_get_client_tokens['entities'].first)
            post "/organizations/#{org.id}/client_tokens", params: { label: 'New Token' }
          end

          it 'does not check configuration complete on success if already configured' do
            config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
            expect(config_complete_checker).to_not receive(:perform_later).with(org.id)
            org.update_attribute(:config_complete, true)
            stub_self_returning_api_client(message: :create_client_token,
                                           response: default_get_client_tokens['entities'].first)
            post "/organizations/#{org.id}/client_tokens", params: { label: 'New Token' }
          end

          it 'fails if no label' do
            post "/organizations/#{org.id}/client_tokens"
            expect(assigns(:organization)).to eq org
            expect(flash[:alert]).to eq("Fields can't be blank.")
            expect(assigns(:errors)).to eq(label: "Label can't be blank.", root: "Fields can't be blank.")
          end

          it 'does not check for complete on failure' do
            config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
            expect(config_complete_checker).to_not receive(:perform_later).with(org.id)
            post "/organizations/#{org.id}/client_tokens"
          end

          it 'shows error if problem' do
            stub_self_returning_api_client(message: :create_client_token,
                                           success: false,
                                           response: nil)
            post "/organizations/#{org.id}/client_tokens", params: { label: 'New Token' }
            expect(flash[:alert]).to eq("We're sorry but we can't complete your request. Please try again tomorrow.")
          end
        end
      end

      describe 'DELETE /destroy' do
        context 'not logged in' do
          it 'redirects to login' do
            delete '/organizations/no-such-id/client_tokens/no-such-id'
            expect(response).to redirect_to('/users/sign_in')
          end
        end

        context 'as cd' do
          let!(:user)      { create_user_with_csp(provider) }
          let(:org_api_id) { SecureRandom.uuid }
          let!(:org) do
            create(:provider_organization, terms_of_service_accepted_by:,
                                           dpc_api_organization_id: org_api_id)
          end

          before do
            create(:cd_org_link, provider_organization: org, user:)
            sign_in user, csp: provider
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
            expect(flash[:success]).to eq('Client token deleted successfully.')
            expect(response).to redirect_to(organization_path(org.id, credential_start: true))
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
            expect(response).to redirect_to(organization_path(org.id, credential_start: true))
          end
        end
      end
    end
  end
end
