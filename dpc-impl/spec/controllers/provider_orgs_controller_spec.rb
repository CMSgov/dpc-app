# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.describe ProviderOrgsController, type: :controller do
  include ApiClientSupport

  describe 'GET #new' do
    context 'authenticated user' do
      it 'returns http success' do
        stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

        user = create(:user)

        sign_in user

        get :new
        expect(response).to have_http_status(:success)
      end
    end
  end

  describe 'GET #add' do
    before(:each) do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      user = create(:user)

      sign_in user
    end

    context 'authenticated user' do
      it 'returns 200' do
        npi = LuhnacyLib.generate_npi
        stub_api_client(message: :create_provider_org, success: true, response: default_provider_org_response)

        post :add, params: { provider_org: { npi: npi } }

        expect(controller.flash[:notice]).to eq("Provider Organization added.")
      end
    end
  end
end