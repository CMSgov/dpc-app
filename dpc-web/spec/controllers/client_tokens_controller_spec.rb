# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ClientTokensController, type: :controller do
  include ApiClientSupport

  describe 'GET #new' do
    let!(:user) { create(:user, :assigned) }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      it 'returns http success' do
        stub = stub_api_client(
          message: :create_organization,
          success: true,
          response: default_org_creation_response
        )
        allow(stub).to receive(:get_public_keys).and_return(stub)
        allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

        org = create(:organization, :api_enabled)
        user.organizations << org

        get :new, params: { organization_id: org.id }
        expect(response).to have_http_status(:success)
      end

      context 'with invalid organization id' do
        it 'redirects to portal' do
          other_org = create(:organization)
          get :new, params: { organization_id: other_org.id }
          expect(response.location).to include(portal_path)
        end
      end
    end
  end

  describe 'GET #destroy' do
    let!(:user) { create(:user, :assigned) }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      context 'with a successful call to the api' do
        it 'returns http success' do
          stub = stub_api_client(
            message: :create_organization,
            success: true,
            response: default_org_creation_response
          )
          allow(stub).to receive(:get_public_keys).and_return(stub)
          allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

          org = create(:organization, :api_enabled)
          user.organizations << org

          allow(stub).to receive(:delete_client_token).and_return(true)

          get :destroy, params: { id: 1, organization_id: org.id }
          expect(response.location).to include(request.host + root_path)
          expect(response).to have_http_status(:found)
        end
      end

      context 'with a failed call to the api' do
        it 'renders new' do
          stub = stub_api_client(
            message: :create_organization,
            success: true,
            response: default_org_creation_response
          )
          allow(stub).to receive(:get_public_keys).and_return(stub)
          allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

          org = create(:organization, :api_enabled)
          user.organizations << org

          allow(stub).to receive(:delete_client_token).and_return(false)

          get :destroy, params: { id: 1, organization_id: org.id }
          expect(response).to render_template(:new)
        end
      end
    end
  end

  describe 'GET #create' do
    let!(:user) { create(:user, :assigned) }
    let!(:organization) { user.organizations.first }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      context 'with invalid params' do
        it 'renders new if no label' do
          stub = stub_api_client(
            message: :create_organization,
            success: true,
            response: default_org_creation_response
          )
          allow(stub).to receive(:get_public_keys).and_return(stub)
          allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

          org = create(:organization, :api_enabled)
          user.organizations << org

          post :create, params: { organization_id: org.id, label: '' }
          expect(response).to render_template(:new)
        end

        it 'redirects to portal if invalid org' do
          other_org = create(:organization)
          post :create, params: { organization_id: other_org.id, label: 'Test', api_environment: 'sandbox' }
          expect(response.location).to include(portal_path)
        end
      end

      context 'with valid params' do
        let!(:user) { create(:user, :assigned) }
        let!(:organization) { user.organizations.first }

        context 'successful API request' do
          before(:each) do
            stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
            reg_org = create(:registered_organization, organization: organization)

            manager = instance_double(ClientTokenManager)
            allow(ClientTokenManager).to receive(:new)
              .with(registered_organization: reg_org)
              .and_return(manager)

            allow(manager).to receive(:create_client_token).with(label: 'Token')
                                                           .and_return(true)
            allow(manager).to receive(:client_token).and_return('1234567890')
          end

          it 'returns 200 and renders show template' do
            post :create, params: { organization_id: organization.id, label: 'Token', api_environment: 'sandbox' }
            expect(response).to have_http_status(:success)
            expect(response).to render_template(:show)
          end
        end

        context 'unsuccessful API request' do
          before(:each) do
            stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
            reg_org = create(:registered_organization, organization: organization)

            manager = instance_double(ClientTokenManager)
            allow(ClientTokenManager).to receive(:new)
              .with(registered_organization: reg_org)
              .and_return(manager)

            allow(manager).to receive(:create_client_token).with(label: 'Token')
                                                           .and_return(false)
          end

          it 'returns error message' do
            post :create, params: { organization_id: organization.id, label: 'Token', api_environment: 'sandbox' }
            expect(response).to render_template(:new)
          end
        end
      end
    end
  end
end
