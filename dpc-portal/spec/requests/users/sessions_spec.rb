# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'Sessions', type: :request do
  include LoginSupport

  describe 'logout' do
    context 'logged in' do
      shared_examples 'logout actions' do |provider|
        let!(:csp) { create(:csp, provider) }
        let!(:user) do
          user = create(:user, provider:)
          create(:csp_user, user:, uuid: SecureRandom.uuid, csp:)
          user
        end
        before { sign_in user, csp: provider }
        it 'should prevent access' do
          delete '/users/sign_out'
          get '/organizations'
          expect(response).to redirect_to('/users/sign_in')
          expect(flash[:alert]).to be_present
        end

        it 'should log action' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['User logged out',
             { actionContext: LoggingConstants::ActionContext::Authentication,
               actionType: LoggingConstants::ActionType::UserLoggedOut }]
          )
          delete '/users/sign_out'
        end

        it 'should redirect to the provider host' do
          delete '/users/sign_out'
          expect(response.location).to include(ENV.fetch("IDP_#{provider.to_s.upcase}_HOST"))
        end
      end

      context 'using Login.gov' do
        include_examples 'logout actions', :login_dot_gov
      end

      context 'using ID.me' do
        include_examples 'logout actions', :id_me
      end
    end

    describe 'Get /auth/logged_out' do
      it 'should redirect to user_return_to' do
        get '/organizations'
        expect(request.session[:user_return_to]).to eq organizations_path
        get '/auth/logged_out'
        expect(response).to redirect_to(organizations_path)
      end

      it 'should redirect to new session if no user_return_to set' do
        get '/auth/logged_out'
        expect(response).to redirect_to(sign_in_path)
      end
    end
  end

  describe 'loads last_used_csp from cookies' do
    let(:last_used_csp) { :login_dot_gov }
    before do
      cookies[:last_used_csp] = last_used_csp.to_s
      get sign_in_path
    end

    # The functionality of which button is wrapped is handled in spec/components/page/session/login_component_spec.rb.
    # Here I just wanted to make sure the cookie is read and the value is passed.
    it 'should set last_used_csp' do
      expect(response.body).to include('last-used-login-wrapper')
    end
  end

  describe 'handles no last_used_csp cookie set' do
    it 'should not wrap a csp button' do
      get sign_in_path
      expect(response.body).not_to include('last-used-login-wrapper')
    end
  end
end
