# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.describe ProviderOrgsController, type: :controller do
  include ApiClientSupport

  describe '#add' do
    before(:each) do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)
  
      user = create(:user)
  
      sign_in user
    end

    context 'successfully adds API provider org' do
      it 'creates success flash notice' do
        stub_api_client(message: :create_provider_org, success: true, response: default_add_provider_org_response)

        npi = LuhnacyLib.generate_npi

        post :add, params: {
          provider_org: {
            npi: npi
          }
        }

        expect(controller.flash[:notice]).to eq("Provider Organization added.")
      end
    end
  end
end