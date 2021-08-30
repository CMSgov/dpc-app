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

        expect(response).to have_http_status(:success)
      end
    end
  end

  describe 'GET #create' do
    let!(:user) { create(:user) }

    before(:each) do
      sign_in user

      stub = stub_api_client(
        message: :create_provider_org, 
        success: true, 
        response: default_add_provider_org_response
      )
      allow(stub).to receive(:response_body).and_return(default_add_provider_org_response)
    end

    # context 'create system' do
    #   post :create, params: {
    #     org_name: 'Intergalatic Pizza',
    #     public_key: file_fixture('stubbed_key.pem').read,
    #     signature: file_fixture('stubbed_signature.pem').read
    #   }
    # end
  end

  describe 'GET #download_snippet' do
    let!(:user) { create(:user) }

    before(:each) do
      sign_in user

      stub = stub_api_client(
        message: :create_provider_org, 
        success: true, 
        response: default_add_provider_org_response
      )
      allow(stub).to receive(:response_body).and_return(default_add_provider_org_response)
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