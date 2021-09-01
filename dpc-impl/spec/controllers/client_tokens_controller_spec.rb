# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ClientTokensController, type: :controller do
  include ApiClientSupport
  let!(:user) { create(:user) }

  describe 'GET #new' do
    context 'authenticated & confirmed user' do
      it 'renders new template with http success' do
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
    before(:each) do
      sign_in user
      @stub = stub_api_client(
        message: :create_provider_org, 
        success: true, 
        response: default_add_provider_org_response
      )
      allow(@stub).to receive(:response_body).and_return(default_add_provider_org_response)
    end

    context 'successful client token creation' do
      it 'returns http success' do
        allow(@stub).to receive(:create_client_token)
          .and_return(default_add_provider_org_response[:org_id],
                      file_fixture('stubbed_token.pem').read,
                      {:label=>"This is a label"})

        post :create, params: {
          label: 'This is a label'
        }

        expect(response).to have_http_status(:success)
      end
    end

    context 'fail to create client token' do
      context 'missing label' do
        it 'renders flash notice' do
          post :create, params: {
            label: ''
          }

          expect(controller.flash[:alert]).to include('Label required.')
        end
      end
    end
  end
end