# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe RegisteredOrganizationsController, type: :controller do
  include DpcClientSupport

  describe '#new' do
    let!(:organization) { create(:organization) }
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API organization new' do
      it 'renders the blank registerd org fields with the org' do

        get :new, params: {
          organization_id: organization.id
        }

        expect(response.status).to eq(200)
        expect(assigns(:organization)).not_to be_nil
        expect(assigns(:registered_organization)).not_to be_nil
        expect(response).to render_template(:new)

        fhir_endpoint = assigns(:registered_organization).fhir_endpoint
        expect(fhir_endpoint.name).to be_nil
        expect(fhir_endpoint.status).to be_nil
        expect(fhir_endpoint.uri).to be_nil
      end
    end

    context 'successful prod-sbx API organization new' do
      it 'renders the blank registerd org fields with the org for prod-sbx' do
        allow(ENV).to receive(:[]).and_call_original
        allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')

        get :new, params: {
          organization_id: organization.id
        }

        expect(response.status).to eq(200)
        expect(assigns(:organization)).not_to be_nil
        expect(assigns(:registered_organization)).not_to be_nil
        expect(response).to render_template(:new)

        fhir_endpoint = assigns(:registered_organization).fhir_endpoint
        expect(fhir_endpoint.name).to eq 'DPC Sandbox Test Endpoint'
        expect(fhir_endpoint.status).to eq 'test'
        expect(fhir_endpoint.uri).to eq 'https://dpc.cms.gov/test-endpoint'
      end
    end
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

        expect(controller.flash[:notice]).to eq("API has been enabled.")
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
          .to include('API could not be enabled:')
        expect(controller.flash[:alert])
          .to include('couldn\'t be registered with API: {"issues"=>["Bad request"]}.')
      end
    end
  end

  describe '#edit' do
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API organization edit' do
      it 'renders the registerd org fields with the org' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        get :edit, params: {
          organization_id: organization.id,
          id: organization.reg_org.id
        }

        expect(response.status).to eq(200)
        expect(assigns(:organization)).not_to be_nil
        expect(assigns(:registered_organization)).not_to be_nil
        expect(response).to render_template(:edit)

        fhir_endpoint = assigns(:registered_organization).fhir_endpoint
        expect(fhir_endpoint.name).to eq 'DPC Sandbox Test Endpoint'
        expect(fhir_endpoint.status).to eq 'test'
        expect(fhir_endpoint.uri).to eq 'https://dpc.cms.gov/test-endpoint'
      end
    end
  end

  describe '#update' do
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API organization edit' do
      it 'creates success flash notice' do
        api_client = stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        api_client = stub_api_client(api_client:, message: :update_organization, success: true, response: default_org_creation_response)
        stub_api_client(api_client:, message: :update_endpoint, success: true, response: default_org_creation_response)

        put :update, params: {
          organization_id: organization.id,
          id: organization.reg_org.id,
          registered_organization: {
            api_env: 'sandbox',
            fhir_endpoint_attributes: { name: 'Endpoint Update', status: 'active', uri: 'http://www.test2.com' }
          }
        }

        expect(controller.flash[:notice]).to eq('Organization successfully updated in API.')
      end
    end

    context 'failed API organization edit' do
      it 'creates failure flash alert with API response text' do
        api_client = stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        stub_api_client(api_client:, message: :update_organization, success: false, response: { 'issues' => ['Bad request'] })

        put :update, params: {
          organization_id: organization.id,
          id: organization.reg_org.id,
          registered_organization: {
            api_env: 'sandbox',
            fhir_endpoint_attributes: { name: 'Endpoint', status: 'active', uri: 'http://www.test.com' }
          }
        }

        expect(response).to render_template(:edit)

        expect(controller.flash[:alert])
          .to include('Organization could not be')
        expect(controller.flash[:alert])
          .to include('updated: couldn\'t be updated (organization) with API: {"issues"=>["Bad request"]}.')
      end
    end
  end

  describe '#destroy' do
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API organization destroy' do
      it 'creates successful destroy flash notice' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        expect(delete :destroy, params: {
          organization_id: organization.id,
          id: organization.reg_org.id
        }).to redirect_to(organization_path(organization))

        expect(controller.flash[:notice]).to eq('API access disabled.')
      end
    end

    context 'failed API organization destroy' do
      it 'creates failure to destroy flash alert with API response text' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        doubled_org = instance_double(Organization)
        doubled_reg_org = instance_double(RegisteredOrganization)
        allow(Organization).to receive(:find).and_return(doubled_org)
        allow(doubled_org).to receive(:registered_organization).and_return(doubled_reg_org)
        allow(doubled_reg_org).to receive(:destroy).and_return(false)
        allow(doubled_reg_org).to receive_message_chain(:errors, :full_messages).and_return(['test', 'message'])

        delete :destroy, params: {
          organization_id: organization.id,
          id: organization.reg_org.id
        }

        expect(controller.flash[:alert])
          .to include('API access could not be')
        expect(controller.flash[:alert])
          .to include('disabled: test, message.')
      end
    end
  end

  describe '#enable_or_disable' do
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API registered organization disable' do
      it 'creates successful destroy flash notice' do
        api_client = stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        api_client = stub_api_client(api_client:, message: :update_organization, success: true, response: default_org_creation_response)
        stub_api_client(api_client:, message: :update_endpoint, success: true, response: default_org_creation_response)

        expect(get :enable_or_disable, params: {
          organization_id: organization.id,
          registered_organization_id: organization.reg_org.id
        }).to redirect_to(organization_path(organization))

        expect(controller.flash[:notice]).to eq('API access disabled.')
      end
    end

    context 'failed API registered organization enable' do
      it 'creates failure to destroy flash alert with API response text' do
        api_client = stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        organization = create(:organization, :api_enabled)

        api_client = stub_api_client(api_client:, message: :update_organization, success: true, response: default_org_creation_response)
        stub_api_client(api_client:, message: :update_endpoint, success: true, response: default_org_creation_response)

        organization.registered_organization.enabled = false
        organization.registered_organization.save

        expect(get :enable_or_disable, params: {
          organization_id: organization.id,
          registered_organization_id: organization.reg_org.id
        }).to redirect_to(organization_path(organization))

        expect(controller.flash[:notice]).to eq('API access enabled.')
      end
    end
  end
end
