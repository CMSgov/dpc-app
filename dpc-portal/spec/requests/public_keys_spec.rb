# frozen_string_literal: true

require 'rails_helper'
require 'support/credential_resource_shared_examples'

RSpec.describe 'PublicKeys', type: :request do
  include DpcClientSupport

  let(:terms_of_service_accepted_by) { create(:user) }

  it_behaves_like 'a credential resource' do
    let(:create_params) do
      { label: 'New Key',
        public_key: file_fixture('stubbed_key.pem').read,
        snippet_signature: 'test snippet signature' }
    end
    let(:credential) { 'public_key' }
  end

  describe 'GET /new' do
    context 'not logged in' do
      it 'redirects to login' do
        get '/organizations/no-such-id/public_keys/new'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'ao access denied' do
      context 'user has sanctions' do
        let!(:user) { create(:user, verification_status: 'rejected', verification_reason: 'ao_med_sanctions') }
        let!(:org) { create(:provider_organization, terms_of_service_accepted_by:) }
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}/public_keys/new"
          expect(response.body).to include(I18n.t('verification.ao_med_sanctions_status'))
          expect(assigns(:organization)).to be_nil
        end
      end

      context 'org has sanctions' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, terms_of_service_accepted_by:, verification_status: 'rejected',
                                         verification_reason: 'org_med_sanctions')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}/public_keys/new"
          expect(response.body).to include(I18n.t('verification.org_med_sanctions_status'))
        end
      end

      context 'org not approved' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, terms_of_service_accepted_by:, verification_status: 'rejected',
                                         verification_reason: 'no_approved_enrollment')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}/public_keys/new"
          expect(response.body).to include(I18n.t('verification.no_approved_enrollment_status'))
        end
      end

      context 'user no longer ao' do
        let!(:user) { create(:user) }
        let!(:org) { create(:provider_organization, terms_of_service_accepted_by:) }
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:, verification_status: false,
                               verification_reason: 'user_not_authorized_official')
          get "/organizations/#{org.id}/public_keys/new"
          expect(response.body).to include(I18n.t('verification.user_not_authorized_official_status'))
        end
      end
    end
    context 'cd access denied' do
      context 'org has sanctions' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, terms_of_service_accepted_by:, verification_status: 'rejected',
                                         verification_reason: 'org_med_sanctions')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:cd_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}/public_keys/new"
          expect(response.body).to include(I18n.t('cd_access.org_med_sanctions_status'))
        end
      end

      context 'org not approved' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, terms_of_service_accepted_by:, verification_status: 'rejected',
                                         verification_reason: 'no_approved_enrollment')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:cd_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}/public_keys/new"
          expect(response.body).to include(I18n.t('cd_access.no_approved_enrollment_status'))
        end
      end
    end

    context 'no link to org' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by:) }
      before { sign_in user }
      it 'redirects to organizations' do
        get "/organizations/#{org.id}/public_keys/new"
        expect(response).to redirect_to('/organizations')
      end
    end

    context :not_signed_tos do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'redirects to organizations page' do
        get "/organizations/#{org.id}/public_keys/new"
        expect(assigns(:organization)).to eq org
        expect(response).to redirect_to(organizations_path)
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by:) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'returns success' do
        get "/organizations/#{org.id}/public_keys/new"
        expect(assigns(:organization)).to eq org
        expect(response).to have_http_status(200)
      end
    end
  end

  describe 'Post /create' do
    context 'not logged in' do
      it 'redirects to login' do
        post '/organizations/no-such-id/public_keys'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    describe 'as cd' do
      let!(:user) { create(:user) }
      let(:org_api_id) { SecureRandom.uuid }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by:, dpc_api_organization_id: org_api_id) }
      let(:success_params) do
        { label: 'New Key',
          public_key: file_fixture('stubbed_key.pem').read,
          snippet_signature: 'test snippet signature' }
      end
      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'succeeds with params' do
        stub_self_returning_api_client(message: :create_public_key,
                                       response: default_get_public_keys['entities'].first)
        post "/organizations/#{org.id}/public_keys", params: success_params
        expect(flash[:success]).to eq('Public key created successfully.')
        expect(assigns(:organization)).to eq org
        expect(response).to redirect_to(organization_path(org, credential_start: true))
      end

      it 'fails on duplicate key' do
        stub_self_returning_api_client(message: :create_public_key,
                                       success: false,
                                       response: 'error: duplicate key value violates unique constraint')
        post "/organizations/#{org.id}/public_keys", params: success_params
        expect(flash[:alert]).to eq(PublicKeyManager::INVALID_KEY)
        expect(assigns(:errors)).to eq(public_key: I18n.t('errors.duplicate_key.text'),
                                       root: PublicKeyManager::INVALID_KEY)
      end

      it 'checks if configuration complete on success' do
        config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
        expect(config_complete_checker).to receive(:perform_later).with(org.id)
        stub_self_returning_api_client(message: :create_public_key,
                                       response: default_get_public_keys['entities'].first)
        post "/organizations/#{org.id}/public_keys", params: success_params
      end

      it 'does not check configuration complete on success if already configured' do
        config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
        expect(config_complete_checker).to_not receive(:perform_later).with(org.id)
        org.update_attribute(:config_complete, true)
        stub_self_returning_api_client(message: :create_public_key,
                                       response: default_get_public_keys['entities'].first)
        post "/organizations/#{org.id}/public_keys", params: success_params
      end

      it 'fails if missing params' do
        post "/organizations/#{org.id}/public_keys"
        expect(flash[:alert]).to eq("Fields can't be blank.")
        expect(assigns(:errors)).to eq(public_key: "Public key can't be blank.",
                                       snippet_signature: "Signature snippet can't be blank.",
                                       label: "Label can't be blank.",
                                       root: "Fields can't be blank.")
      end

      it 'does not check for complete on failure' do
        config_complete_checker = class_double('CheckConfigCompleteJob').as_stubbed_const
        expect(config_complete_checker).to_not receive(:perform_later).with(org.id)
        post "/organizations/#{org.id}/public_keys"
      end

      it 'fails if label over 25 characters' do
        stub_api_client(message: :get_organization,
                        response: default_get_org_response(org_api_id))
        post "/organizations/#{org.id}/public_keys", params: {
          label: 'aaaaabbbbbcccccdddddeeeeefffff',
          public_key: file_fixture('stubbed_key.pem').read,
          snippet_signature: 'test snippet signature'
        }
        expect(flash[:alert]).to eq('Invalid label.')
        expect(assigns(:errors)).to eq(label: 'Label must be 25 characters or fewer.', root: 'Invalid label.')
      end

      it 'shows error if problem' do
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :create_public_key,
                                       success: false,
                                       response: nil,
                                       api_client:)
        post "/organizations/#{org.id}/public_keys", params: {
          label: 'New Key',
          public_key: file_fixture('stubbed_key.pem').read,
          snippet_signature: 'test snippet signature'
        }
        expect(flash[:alert]).to eq("We're sorry, but we can't complete your request. Please try again tomorrow.")
      end
    end
  end

  describe 'Delete /destroy' do
    context 'not logged in' do
      it 'redirects to login' do
        delete '/organizations/no-such-id/public_keys/no-such-id'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let(:org_api_id) { SecureRandom.uuid }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by:, dpc_api_organization_id: org_api_id) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'flashes success if succeeds' do
        key_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :delete_public_key,
                                       response: nil,
                                       with: [org_api_id, key_guid],
                                       api_client:)
        delete "/organizations/#{org.id}/public_keys/#{key_guid}"
        expect(flash[:success]).to eq('Public key deleted successfully.')
        expect(response).to redirect_to(organization_path(org, credential_start: true))
      end

      it 'renders error if error' do
        key_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: :delete_public_key,
                                       response: nil,
                                       success: false,
                                       with: [org_api_id, key_guid],
                                       api_client:)
        delete "/organizations/#{org.id}/public_keys/#{key_guid}"
        expect(flash[:alert]).to eq('Public key could not be deleted.')
      end
    end
  end
end
