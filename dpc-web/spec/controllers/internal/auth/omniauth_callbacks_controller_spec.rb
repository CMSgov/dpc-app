# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Internal::Auth::OmniauthCallbacksController, type: :controller do
  include OauthSupport

  describe '#oktaoauth' do
    around do |example|
      OmniAuth.config.test_mode = true

      ClimateControl.modify INTERNAL_AUTH_PROVIDER: 'oktaoauth', OKTA_ADMIN_ROLE: 'DPC_AppRole_Admin' do
        example.run
      end

      OmniAuth.config.mock_auth[:oktaoauth] = nil
    end

    before(:each) do
      request.env["devise.mapping"] = Devise.mappings[:internal_user]
    end

    context 'Okta not enabled' do
      it 'redirects to login page with alert' do
        stub_const('InternalUser::OKTA_AUTH_ENABLED', false)

        post :oktaoauth
        expect(response.location).to include(new_internal_user_session_path)
        expect(flash[:alert]).to eq('Not allowed.')
      end
    end

    context 'Okta enabled' do
      before(:each) do
        stub_const('InternalUser::OKTA_AUTH_ENABLED', true)
      end

      context 'valid user' do
        it 'assigns @internal_user and redirects to signed in path' do
          mock_oktaoauth
          set_omniauth_request_env(:oktaoauth)

          post :oktaoauth

          expect(response.location).to  include(internal_users_path)
          expect(assigns(:internal_user)).to be_present
          expect(flash[:notice]).to be_present
        end
      end

      context 'insufficient LOA level' do
        it 'redirects to login page with alert' do
          mock_oktaoauth(loa: '2')
          set_omniauth_request_env(:oktaoauth)

          post :oktaoauth

          expect(response.location).to  include(new_internal_user_session_path)
          expect(assigns(:internal_user)).not_to be_present
          expect(flash[:alert]).to be_present
        end
      end

      context 'insufficient roles' do
        it 'redirects to login page with alert' do
          mock_oktaoauth(roles: ['BadRole'])
          set_omniauth_request_env(:oktaoauth)

          post :oktaoauth

          expect(response.location).to  include(new_internal_user_session_path)
          expect(assigns(:internal_user)).not_to be_present
          expect(flash[:alert]).to be_present
        end
      end

      context 'oauth failure' do
        it 'redirects to login page with alert' do
          OmniAuth.config.mock_auth[:oktaoauth] = :invalid_credentials

          post :oktaoauth

          expect(response.location).to  include(new_internal_user_session_path)
          expect(assigns(:internal_user)).not_to be_present
          expect(flash[:alert]).to be_present
        end
      end
    end
  end
end
