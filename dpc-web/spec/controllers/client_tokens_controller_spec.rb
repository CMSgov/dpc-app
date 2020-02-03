require 'rails_helper'

RSpec.describe ClientTokensController, type: :controller do
  include APIClientSupport

  describe "GET #new" do
    let!(:user) { create(:user, :assigned) }
    let!(:organization) { user.organizations.first }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      it "returns http success" do
        get :new, params: { organization_id: organization.id }
        expect(response).to have_http_status(:success)
      end

      context 'with invalid organization id' do
        it 'redirects to dashboard' do
          other_org = create(:organization)
          get :new, params: { organization_id: other_org.id }
          expect(response.location).to include(dashboard_path)
        end
      end
    end
  end

  describe "GET #create" do
    let!(:user) { create(:user, :assigned) }
    let!(:organization) { user.organizations.first }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      context 'with invalid params' do
        it 'renders new if no label' do
          post :create, params: { organization_id: organization.id, label: '', api_environment: 'sandbox' }
          expect(response).to render_template(:new)
        end

        it 'renders new if no API env' do
          post :create, params: { organization_id: organization.id, label: 'Test', api_environment: nil }
          expect(response).to render_template(:new)
        end

        it 'redirects to dashboard if invalid org' do
          other_org = create(:organization)
          post :create, params: { organization_id: other_org.id, label: 'Test', api_environment: 'sandbox' }
          expect(response.location).to include(dashboard_path)
        end
      end

      context 'with valid params' do
        context 'successful API request' do
          before(:each) do
            stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
            reg_org = create(:registered_organization, api_env: 'sandbox', organization: organization)

            manager = instance_double(ClientTokenManager)
            allow(ClientTokenManager).to receive(:new)
              .with(api_env: 'sandbox', registered_organization: reg_org)
              .and_return(manager)

            allow(manager).to receive(:create_client_token).with(label: 'Token')
              .and_return(true)
            allow(manager).to receive(:client_token).and_return('1234567890')
          end

          it "returns 200 and renders show template" do
            post :create, params: { organization_id: organization.id, label: 'Token', api_environment: 'sandbox' }
            expect(response).to have_http_status(:success)
            expect(response).to render_template(:show)
          end
        end

        context 'unsuccessful API request' do
          before(:each) do
            stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
            reg_org = create(:registered_organization, api_env: 'sandbox', organization: organization)

            manager = instance_double(ClientTokenManager)
            allow(ClientTokenManager).to receive(:new)
              .with(api_env: 'sandbox', registered_organization: reg_org)
              .and_return(manager)

            allow(manager).to receive(:create_client_token).with(label: 'Token')
              .and_return(false)
          end

          it "returns error message" do
            post :create, params: { organization_id: organization.id, label: 'Token', api_environment: 'sandbox' }
            expect(response).to render_template(:new)
          end
        end
      end
    end
  end

end
