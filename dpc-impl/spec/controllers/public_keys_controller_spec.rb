# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeysController, type: :controller do
  include ApiClientSupport

  describe 'GET #create' do
    let!(:user) { create(:user) }
    
    context 'authenticated & confirmed user' do
      it 'assigns' do
        sign_in user

        stub = stub_api_client(
          message: :create_provider_org, 
          success: true, 
          response: default_add_provider_org_response
        )
        all(stub).to receive(:response_body).and_return(default_add_provider_org_response)
        binding.pry
      end
    end
  end
end