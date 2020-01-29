# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe Internal::OrganizationsController, type: :controller do
  describe '#new' do
    it_behaves_like 'an internal user authenticable controller action', :get, :new
  end

  describe '#create' do
    let!(:internal_user) { create(:internal_user) }
    let!(:organization) { create(:organization, name: 'Old Name') }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    context 'successful API organization creation' do
      xit 'creates success flash notice' do
        post :create, params: {
          registered_organization: {
            api_env: 'sandbox',
            fhir_endpoint_attributes: { name: 'Endpoint', status: 'Active', endpoint: 'http://test.com' }
          }
        }

        expect(controller.flash).to eq("Access to sandbox enabled.")
      end
    end

    context 'failed API organization creation' do
      xit 'creates failure flash alert with API response text'
    end
  end

  describe '#edit' do
    it_behaves_like 'an internal user authenticable controller action', :get, :edit, :registered_organization
  end

  describe '#update' do
    it_behaves_like 'an internal user authenticable controller action',
                    :put, :update, :registered_organization, params: {
                      registered_organization: {
                        fhir_endpoint_attributes: {
                          name: 'Good Endpoint'
                        }
                      }
                    }
  end
end