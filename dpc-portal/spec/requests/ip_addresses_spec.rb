# frozen_string_literal: true

require 'rails_helper'
require 'support/credential_resource_shared_examples'
require 'support/login_support'

RSpec.describe 'IpAddresses', type: :request do
  include DpcClientSupport
  include LoginSupport

  let(:terms_of_service_accepted_by) { create(:user) }

  it_behaves_like 'a credential resource' do
    let(:credential) { 'ip_address' }
    let(:create_params) { { ip_address: '136.226.19.87' } }
  end

  LoginSupport::CSP_MAP.each do |provider, display_name|
    context "using #{display_name}" do
      describe 'GET /new' do
        new_path = ->(org) { "/organizations/#{org.id}/ip_addresses/new" }
        context 'not logged in' do
          it 'redirects to login' do
            get '/organizations/no-such-id/ip_addresses/new'
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
            post '/organizations/no-such-id/ip_addresses'
            expect(response).to redirect_to('/users/sign_in')
          end
        end
        context 'as cd' do
          let!(:user) { create_user_with_csp(csp: provider) }
          let(:org_api_id) { SecureRandom.uuid }
          let!(:org) do
            create(:provider_organization, terms_of_service_accepted_by:, dpc_api_organization_id: org_api_id)
          end

          before do
            create(:cd_org_link, provider_organization: org, user:)
            sign_in user, csp: provider
          end

          it 'succeeds with valid params' do
            stub_self_returning_api_client(message: :create_ip_address,
                                           response: default_get_ip_addresses['entities'].first)
            post "/organizations/#{org.id}/ip_addresses", params: { ip_address: '136.226.19.87' }
            expect(response).to redirect_to(organization_path(org, credential_start: true))
            expect(assigns(:organization)).to eq org
            expect(flash[:success]).to eq('Public IP address created successfully.')
          end

          it 'checks if configuration complete on success' do
            config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
            expect(config_complete_checker).to receive(:perform_later).with(org.id)
            stub_self_returning_api_client(message: :create_ip_address,
                                           response: default_get_ip_addresses['entities'].first)
            post "/organizations/#{org.id}/ip_addresses", params: { ip_address: '136.226.19.87' }
          end
          it 'does not check for complete if complete = true' do
            config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
            expect(config_complete_checker).to_not receive(:perform_later).with(org.id)
            org.update_attribute(:config_complete, true)
            stub_self_returning_api_client(message: :create_ip_address,
                                           response: default_get_ip_addresses['entities'].first)
            post "/organizations/#{org.id}/ip_addresses", params: { ip_address: '136.226.19.87' }
          end
          it 'fails if missing params' do
            post "/organizations/#{org.id}/ip_addresses"
            expect(assigns(:organization)).to eq org
            expect(flash[:alert]).to eq('IP address invalid.')
            expect(assigns[:errors]).to eq(ip_address: "IP address can't be blank.")
          end

          it 'does not check for complete on failure' do
            config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
            expect(config_complete_checker).to_not receive(:perform_later).with(org.id)
            post "/organizations/#{org.id}/ip_addresses"
          end
          it 'fails if invalid IP' do
            stub_self_returning_api_client(message: :create_ip_address,
                                           response: default_get_ip_addresses['entities'].first)
            post "/organizations/#{org.id}/ip_addresses", params: { ip_address: '333.333.333.333' }
            expect(assigns(:organization)).to eq org
            error_msg = 'Invalid IP address.'
            expect(flash[:alert]).to eq(error_msg)
            expect(assigns[:errors]).to eq(ip_address: error_msg, root: error_msg)
          end

          it 'shows error if problem' do
            stub_self_returning_api_client(message: :create_ip_address,
                                           success: false,
                                           response: nil)
            post "/organizations/#{org.id}/ip_addresses", params: { ip_address: '136.226.19.87' }
            expect(flash[:alert]).to eq("We're sorry but we can't complete your request. Please try again tomorrow.")
          end
        end
      end
      describe 'DELETE /destroy' do
        context 'not logged in' do
          it 'redirects to login' do
            delete '/organizations/no-such-id/ip_addresses/no-such-id'
            expect(response).to redirect_to('/users/sign_in')
          end
        end
        context 'as cd' do
          let!(:user) { create_user_with_csp(csp: provider) }
          let(:org_api_id) { SecureRandom.uuid }
          let!(:org) do
            create(:provider_organization, terms_of_service_accepted_by:, dpc_api_organization_id: org_api_id)
          end

          before do
            create(:cd_org_link, provider_organization: org, user:)
            sign_in user, csp: provider
          end

          it 'flashes success if succeeds' do
            addr_guid = SecureRandom.uuid
            stub_self_returning_api_client(message: :delete_ip_address,
                                           response: nil,
                                           with: [org_api_id, addr_guid])
            delete "/organizations/#{org.id}/ip_addresses/#{addr_guid}"
            expect(flash[:success]).to eq('Public IP address deleted successfully.')
            expect(response).to redirect_to(organization_path(org, credential_start: true))
          end

          it 'renders error if error' do
            addr_guid = SecureRandom.uuid
            stub_self_returning_api_client(message: :delete_ip_address,
                                           response: nil,
                                           success: false,
                                           with: [org_api_id, addr_guid])
            delete "/organizations/#{org.id}/ip_addresses/#{addr_guid}"
            expect(flash[:alert]).to eq('Public IP address could not be deleted.')
            expect(response).to redirect_to(organization_path(org, credential_start: true))
          end
        end
      end
    end
  end
end
