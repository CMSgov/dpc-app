# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Organizations', type: :request do
  include DpcClientSupport

  describe 'GET /index not logged in' do
    it 'redirects to login' do
      get '/organizations'
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'GET /index' do
    let!(:user) { create(:user) }
    before { sign_in user }

    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get '/organizations', params: { id: api_id }
    end
  end

  describe 'GET /organizations/[organization_id] not logged in' do
    it 'redirects to login' do
      get '/organizations/no-such-id'
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'GET /organizations/[organization_id]' do
    let!(:user) { create(:user) }
    before { sign_in user }

    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_client(api_id)
      get "/organizations/#{api_id}"
      expect(assigns(:organization).api_id).to eq api_id
    end

    it 'redirects if prod-sbx' do
      api_id = SecureRandom.uuid
      allow(ENV)
        .to receive(:fetch)
        .with('ENV', nil)
        .and_return('prod-sbx')
      get "/organizations/#{api_id}"
      expect(response).to redirect_to(root_url)
    end

    it 'goes to hard-coded org if test' do
      api_id = SecureRandom.uuid
      allow(ENV)
        .to receive(:fetch)
        .with('ENV', nil)
        .and_return('test')
      hard_coded_id = '6a1dbf47-825b-40f3-b81d-4a7ffbbdc270'
      stub_client(hard_coded_id)
      get "/organizations/#{api_id}"
      expect(assigns(:organization).api_id).to eq hard_coded_id
    end

    it 'goes to hard-coded org if dev' do
      api_id = SecureRandom.uuid
      allow(ENV)
        .to receive(:fetch)
        .with('ENV', nil)
        .and_return('dev')
      hard_coded_id = '78d02106-2837-4d07-8c51-8d73332aff09'
      stub_client(hard_coded_id)
      get "/organizations/#{api_id}"
      expect(assigns(:organization).api_id).to eq hard_coded_id
    end
  end

  describe 'GET /organizations/[organization_id]?ao=true' do
    let!(:user) { create(:user) }
    before { sign_in user }

    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_client(api_id)
      get "/organizations/#{api_id}?ao=true"
      expect(assigns(:organization).api_id).to eq api_id
    end

    it 'assigns invitations if exist' do
      api_id = SecureRandom.uuid
      stub_client(api_id)
      provider_organization = create(:provider_organization, dpc_api_organization_id: api_id)
      create(:invitation, provider_organization:, invited_by: user)
      get "/organizations/#{api_id}?ao=true"
      expect(assigns(:invitations).size).to eq 1
    end

    it 'does not assign invitations if not exist' do
      api_id = SecureRandom.uuid
      stub_client(api_id)
      get "/organizations/#{api_id}?ao=true"
      expect(assigns(:invitations).size).to eq 0
    end
  end

  describe 'AO org flow' do
    let!(:user) { create(:user) }
    before { sign_in user }

    context 'GET /organizations/new' do
      it 'returns success' do
        SecureRandom.uuid
        get '/organizations/new'
        expect(response).to be_ok
      end
    end

    context 'POST /organizations' do
      context 'with valid input' do
        it 'creates new org if none exists' do
          npi = '1111111111'
          expect do
            post '/organizations', params: { npi: }
          end.to change { ProviderOrganization.count }.by 1
          org = assigns(:organization)
          expect(org.npi).to eq npi
          expect(org.name).to eq "Org with npi #{npi}"
          expect(org.terms_of_service_accepted_by).to be_nil
          expect(org.terms_of_service_accepted_at).to be_nil
          expect(response).to redirect_to(tos_form_organization_path(org))
        end

        it 'creates new ao-org-link if none exists' do
          npi = '1111111111'
          expect do
            post '/organizations', params: { npi: }
          end.to change { AoOrgLink.count }.by 1
          link = assigns(:ao_org_link)
          expect(link.provider_organization).to eq assigns(:organization)
          expect(link.user).to eq user
        end

        it 'does not create new org if exists' do
          npi = '1111111111'
          name = 'Health Hut'
          create(:provider_organization, npi:, name:)
          expect do
            post '/organizations', params: { npi: }
          end.to change { ProviderOrganization.count }.by 0
          org = assigns(:organization)
          expect(org.npi).to eq npi
          expect(org.name).to eq name
          expect(org.terms_of_service_accepted_by).to be_nil
          expect(org.terms_of_service_accepted_at).to be_nil
          expect(response).to redirect_to(tos_form_organization_path(org))
        end

        it 'redirects to success if org has signed tos' do
          npi = '1111111111'
          create(:provider_organization, npi:, terms_of_service_accepted_at: 1.day.ago)
          expect do
            post '/organizations', params: { npi: }
          end.to change { ProviderOrganization.count }.by 0
          org = assigns(:organization)
          expect(response).to redirect_to(success_organization_path(org))
        end
      end

      it 'fails if blank' do
        post '/organizations', params: { npi: '' }
        expect(response).to be_bad_request
        expect(assigns(:npi_error)).to eq "can't be blank"
      end

      it 'fails if not 10 digits' do
        post '/organizations', params: { npi: '22' }
        expect(response).to be_bad_request
        expect(assigns(:npi_error)).to eq 'length has to be 10'
      end
    end

    context 'GET /organizations/[organization_id]/tos_form' do
      it 'renders tos form' do
        org = create(:provider_organization)
        get "/organizations/#{org.id}/tos_form"
        expect(response).to be_ok
      end
      it 'fails if no org' do
        get '/organizations/fake-org/tos_form'
        expect(response).to be_not_found
      end
    end
    context 'POST /organizations/[organization_id]/sign_tos' do
      it 'succeeds' do
        org = create(:provider_organization)
        post "/organizations/#{org.id}/sign_tos"
        org.reload
        expect(org.terms_of_service_accepted_at).to be_present
        expect(org.terms_of_service_accepted_by).to eq user
        expect(response).to redirect_to(success_organization_path(org))
      end
    end
    context 'GET /organizations/[organization_id]/success' do
      it 'shows success page' do
        org = create(:provider_organization)
        get "/organizations/#{org.id}/success"
        expect(response).to be_ok
      end
      it 'fails if no org' do
        get '/organizations/fake-org/success'
        expect(response).to be_not_found
      end
    end
  end

  def stub_client(api_id)
    client = stub_api_client(message: :get_organization,
                             response: default_get_org_response(api_id))
    stub_self_returning_api_client(message: :get_client_tokens,
                                   response: default_get_client_tokens,
                                   api_client: client)
    stub_self_returning_api_client(message: :get_public_keys,
                                   response: default_get_public_keys,
                                   api_client: client)
    stub_self_returning_api_client(message: :get_ip_addresses,
                                   response: default_get_ip_addresses,
                                   api_client: client)
  end
end
