# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeysController, type: :controller do
  include DpcClientSupport

  describe 'GET #new' do
    let!(:user) { create(:user, :assigned) }
    let(:org) { create(:organization, :api_enabled) }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user

        stub = stub_api_client(
          message: :create_organization,
          success: true,
          response: default_org_creation_response
        )
        allow(stub).to receive(:get_public_keys).and_return(stub)
        allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

        user.organizations << org
      end

      it 'assigns the correct organization' do
        get :new, params: {
          organization_id: org.id
        }

        expect(response.status).to eq(200)
        expect(assigns(:organization)).not_to be_nil
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

          allow(stub).to receive(:delete_public_key).and_return(true)

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

          allow(stub).to receive(:delete_public_key).and_return(false)

          get :destroy, params: { id: 1, organization_id: org.id }
          expect(response).to render_template(:new)

          expect(flash[:alert]).to_not be_nil
        end
      end
    end
  end

  describe 'GET #create' do
    let!(:user) { create(:user, :assigned) }
    let(:org) { create(:organization, :api_enabled) }

    before(:each) do
      sign_in user, scope: :user

      stub = stub_api_client(
        message: :create_organization,
        success: true,
        response: default_org_creation_response
      )
      allow(stub).to receive(:get_public_keys).and_return(stub)
      allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

      user.organizations << org
    end

    context 'when missing a public key param' do
      it 'renders an error' do
        post :create, params: {
          organization_id: org.id,
          label: ''
        }

        expect(response).to render_template(:new)

        expect(controller.flash[:alert])
          .to include('Required values missing.')
      end
    end

    context 'when label is greater than 25' do
      it 'renders an error' do
        post :create, params: {
          organization_id: org.id,
          public_key: 'test key',
          label: 'aaaaabbbbbcccccdddddeeeeefffff'
        }

        expect(response).to render_template(:new)

        expect(controller.flash[:alert])
          .to include('Label cannot be over 25 characters')
      end
    end

    context 'when the PublicKeyManager raises an error' do
      it 'renders an error' do
        doubled_manager = instance_double(PublicKeyManager)
        allow(PublicKeyManager).to receive(:new).and_return(doubled_manager)
        allow(doubled_manager).to receive(:create_public_key).and_return(
          {
            response: false,
            message: 'test message'
          }
        )

        post :create, params: {
          organization_id: org.id,
          public_key: 'test key',
          label: 'aaaaabbbbbcccccddddd'
        }

        expect(response).to render_template(:new)

        expect(controller.flash[:alert])
          .to include('test message')
      end
    end

    context 'when the user creates a new public key' do
      it 'redirects to the portal path successfully' do
        doubled_manager = instance_double(PublicKeyManager)
        allow(PublicKeyManager).to receive(:new).and_return(doubled_manager)
        allow(doubled_manager).to receive(:create_public_key).and_return(
          {
            response: true,
            message: 'test message'
          }
        )

        expect((post :create, params: {
          organization_id: org.id,
          public_key: 'test key',
          label: 'aaaaabbbbbcccccddddd'
        })).to redirect_to(portal_path)
      end
    end
  end

  describe 'GET #download_snippet' do
    let!(:user) { create(:user, :assigned) }
    let(:org) { create(:organization) }

    before(:each) do
      sign_in user, scope: :user

      stub = stub_api_client(
        message: :create_organization,
        success: true,
        response: default_org_creation_response
      )
      allow(stub).to receive(:get_public_keys).and_return(stub)
      allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

      user.organizations << org
    end

    context 'when the snippet is requested' do
      it 'serves the snippet file' do
        post :download_snippet, params: {}

        expect(response.status).to eq(202)
        expect(response.header['Content-Type']).to eq('application/zip')
        expect(response.body).to eq('This is the snippet used to verify a key pair in DPC.')
      end
    end
  end

  describe 'GET #organization_enabled?' do
    let!(:user) { create(:user, :assigned) }
    let(:org) { create(:organization) }

    before(:each) do
      sign_in user, scope: :user

      stub = stub_api_client(
        message: :create_organization,
        success: true,
        response: default_org_creation_response
      )
      allow(stub).to receive(:get_public_keys).and_return(stub)
      allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

      user.organizations << org
    end

    context 'when the organization is not enabled' do
      it 'redirects to the root path' do
        expect((get :new, params: {
          organization_id: org.id
        })).to redirect_to(root_path)
      end
    end
  end

  context 'When a record not found error is encountered' do
    let!(:user) { create(:user, :assigned) }

    before(:each) do
      sign_in user, scope: :user
    end

    it 'renders an error and redirects to portal' do
      expect(controller).to receive(:organization_enabled?).and_raise(ActiveRecord::RecordNotFound)

      expect((get :new, params: {
        organization_id: '1'
      })).to redirect_to(portal_path)
    end
  end
end
