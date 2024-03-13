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

    it 'returns success if no orgs associated with user' do
      get '/organizations'
      expect(assigns(:organizations)).to be_empty
    end

    it 'returns organizations linked to user as ao'
    it 'returns organizations linked to user as cd'
  end

  describe 'GET /organizations/[organization_id] not logged in' do
    it 'redirects to login' do
      org = create(:provider_organization)
      get "/organizations/#{org.id}"
      expect(response).to redirect_to('/portal/users/sign_in')
    end
  end

  describe 'GET /organizations/[organization_id] not own' do
    let!(:user) { create(:user) }
    before { sign_in user }
    it 'redirects to organizations page' do
      org = create(:provider_organization)
      get "/organizations/#{org.id}"
      expect(response).to redirect_to(organizations_path)
    end
  end

  describe 'GET /organizations/[organization_id]' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization) }
    let!(:link) { create(:ao_org_link, user:, provider_organization: org) }
    before { sign_in user }

    it 'returns success' do
      get "/organizations/#{org.id}"
      expect(assigns(:organization)).to eq org
    end

    it 'redirects if prod-sbx' do
      allow(ENV)
        .to receive(:fetch)
              .with('ENV', nil)
              .and_return('prod-sbx')
      get "/organizations/#{org.id}"
      expect(assigns(:organization)).to be_nil
      expect(response).to redirect_to(root_url)
    end
  end

  describe 'GET /organizations/[organization_id]?ao=true' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization) }
    let!(:link) { create(:ao_org_link, user:, provider_organization: org) }
    before { sign_in user }

    it 'returns success' do
      get "/organizations/#{org.id}?ao=true"
      expect(assigns(:organization)).to eq org
    end

    context :invitations do
      it 'assigns if exist' do
        create(:invitation, provider_organization: org, invited_by: user)
        get "/organizations/#{org.id}?ao=true"
        expect(assigns(:invitations).size).to eq 1
      end

      it 'does not assign if not exist' do
        get "/organizations/#{org.id}?ao=true"
        expect(assigns(:invitations).size).to eq 0
      end
    end

    context :credential_delegates do
      it 'assigns if exist' do
        create(:cd_org_link, provider_organization: org)
        get "/organizations/#{org.id}?ao=true"
        expect(assigns(:cds).size).to eq 1
      end

      it 'does not assign if not exist' do
        get "/organizations/#{org.id}?ao=true"
        expect(assigns(:invitations).size).to eq 0
      end

      it 'does not assign if link disabled' do
        create(:cd_org_link, provider_organization: org, disabled_at: 1.day.ago)
        get "/organizations/#{org.id}?ao=true"
        expect(assigns(:invitations).size).to eq 0
      end
    end
  end
end
