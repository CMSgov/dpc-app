# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeysController, type: :controller do
  include ApiClientSupport

  describe 'GET #new' do
    let!(:user) { create(:user) }
    
    context 'authenticated & confirmed user' do
      it 'returns http success' do
        sign_in user

        stub = stub_api_client(
          message: :create_provider_org, 
          success: true, 
          response: default_add_provider_org_response
        )
        allow(stub).to receive(:response_body).and_return(default_add_provider_org_response)

        render_template(:new)
        expect(response).to have_http_status(:success)
      end
    end
  end

  describe 'GET #create' do
    let!(:user) { create(:user) }

    before(:each) do
      sign_in user

      @stub = stub_api_client(
        message: :create_provider_org, 
        success: true, 
        response: default_add_provider_org_response
      )
      allow(@stub).to receive(:response_body).and_return(default_add_provider_org_response)
    end

    context 'successfully creates system' do
      it 'returns http success' do
        allow(@stub).to receive(:create_system)
          .and_return(default_add_provider_org_response[:org_id],
                      file_fixture('stubbed_token.pem').read,
                      {:params=>{:client_name=>"Intergalatic Pizza System", :public_key=>file_fixture('stubbed_key.pem').read}})

        post :create, params: {
          org_name: 'Intergalatic Pizza',
          public_key: file_fixture('stubbed_key.pem').read,
          signature: file_fixture('stubbed_signature.pem').read
        }

        expect(response).to have_http_status(:success)
      end
    end

    context 'when missing a public key param' do
      it 'renders a relevant error' do
        post :create, params: {
          org_name: 'Intergalatic Pizza',
          public_key: '',
          signature: ''
        }

        expect(controller.flash[:alert]).to include('Required values missing.')
      end
    end
  end

  describe 'GET #destroy' do
    let!(:user) { create(:user) }

    before(:each) do
      sign_in user

      @stub = stub_api_client(
        message: :create_provider_org, 
        success: true, 
        response: default_add_provider_org_response
      )
      allow(@stub).to receive(:response_body).and_return(default_add_provider_org_response)
    end

    context 'successfully deletes public key' do
      it 'returns http success' do
        allow(@stub).to receive(:delete_public_key).and_return(true)

        get :destroy, params: { id: 1 }
        expect(response.location).to include(request.host + root_path)
        expect(response).to have_http_status(:found)
      end
    end

    context 'cannot delete public key' do
      it 'renders flash notice' do
        allow(@stub).to receive(:delete_public_key).and_return(false)

        get :destroy, params: { id: 1 }

        expect(controller.flash[:alert]).to include('Public key could not be deleted.')
      end
    end
  end

  describe 'GET #download_snippet' do
    let!(:user) { create(:user) }

    before(:each) do
      sign_in user
    end

    context 'when the snippet is requested' do
      it 'serves the snippet file' do
        post :download_snippet, params: {}

        expect(response.status).to eq(202)
        expect(response.header['Content-Type']).to eq('application/zip')
        expect(response.body).to eq('This is the snippet used to verify a key pair.')
      end
    end
  end
end