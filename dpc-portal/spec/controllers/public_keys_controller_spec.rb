# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeysController, type: :controller do
  include DpcClientSupport

  org_id = SecureRandom.uuid

  describe 'GET #new' do
    context 'user' do
      it 'assigns the correct organization' do
        stub_api_client(
          message: :get_organization,
          response: default_get_org_response(org_id)
        )

        get :new, params: {
          organization_id: org_id
        }

        expect(response.status).to eq(200)
      end
    end
  end

  describe 'GET #destroy' do
    context 'user' do
      context 'with a successful call to the api' do
        it 'returns http success' do
          stub = stub_api_client(
            message: :get_organization,
            response: default_get_org_response(org_id)
          )
          allow(stub).to receive(:delete_public_key).and_return(true)

          get :destroy, params: { id: 1, organization_id: org_id }
          expect(response.location).to include(request.host + root_path)
          expect(response).to have_http_status(:found)
        end
      end

      context 'with a failed call to the api' do
        it 'renders new' do
          stub = stub_api_client(
            message: :get_organization,
            response: default_get_org_response(org_id)
          )
          allow(stub).to receive(:delete_public_key).and_return(false)

          get :destroy, params: { id: 1, organization_id: org_id }
          expect(response).to render_template(:new)

          expect(flash[:alert]).to_not be_nil
        end
      end
    end
  end

  describe 'GET #create' do
    context 'when missing a public key param' do
      it 'renders an error' do
        stub_api_client(
          message: :get_organization,
          response: default_get_org_response(org_id)
        )

        post :create, params: {
          organization_id: org_id,
          label: ''
        }

        expect(response).to render_template(:new)

        expect(controller.flash[:alert])
          .to include('Required values missing.')
      end
    end

    context 'when label is greater than 25' do
      it 'renders an error' do
        stub_api_client(
          message: :get_organization,
          response: default_get_org_response(org_id)
        )

        post :create, params: {
          organization_id: org_id,
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
          organization_id: org_id,
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
          organization_id: org_id,
          public_key: 'test key',
          label: 'aaaaabbbbbcccccddddd'
        })).to redirect_to(portal_path)
      end
    end
  end

  describe 'GET #download_snippet' do
    context 'when the snippet is requested' do
      it 'serves the snippet file' do
        post :download_snippet, params: {}
        expect(response.status).to eq(202)
        expect(response.header['Content-Type']).to eq('application/zip')
        expect(response.body).to eq('This is the snippet used to verify a key pair in DPC.')
      end
    end
  end
end
