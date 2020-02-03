# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe Internal::RegisteredOrganizationsController, type: :controller do
  include APIClientSupport

  describe '#new' do
    # FIXME don't hardcode IDs in tests - will require refactoring shared examples
    it_behaves_like 'an internal user authenticable controller action', :get, :new, nil, params: { organization_id: 1 }
  end

  describe '#create' do
    let!(:organization) { create(:organization) }
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API organization creation' do
      it 'creates success flash notice' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        post :create, params: {
          organization_id: organization.id,
          registered_organization: {
            api_env: 'sandbox',
            fhir_endpoint_attributes: { name: 'Endpoint', status: 'active', uri: 'http://www.test.com' }
          }
        }

        expect(controller.flash[:notice]).to eq("Access to sandbox enabled.")
      end
    end

    context 'failed API organization creation' do
      it 'creates failure flash alert with API response text' do
        stub_api_client(message: :create_organization, success: false, response: { 'issues' => ['Bad request'] })

        post :create, params: {
          organization_id: organization.id,
          registered_organization: {
            api_env: 'sandbox',
            fhir_endpoint_attributes: { name: 'Endpoint', status: 'active', uri: 'http://www.test.com' }
          }
        }

        expect(response).to render_template(:new)
        # Separating to ignore whitespace discrepancy
        expect(controller.flash[:alert])
          .to include('Access to sandbox could not be enabled:')
        expect(controller.flash[:alert])
          .to include('couldn\'t be registered with sandbox API: {"issues"=>["Bad request"]}.')
      end
    end
  end

  describe '#edit' do
    before(:each) do
      stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
    end

    it_behaves_like 'an internal user authenticable controller action', :get, :edit, :registered_organization, params: {organization_id: 1}
  end

  describe '#update' do
    before(:each) do
      stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
    end

    it_behaves_like 'an internal user authenticable controller action',
                    :put, :update, :registered_organization, params: {
                      organization_id: 1,
                      registered_organization: {
                        fhir_endpoint_attributes: {
                          name: 'Good Endpoint'
                        }
                      }
                    }
  end
end