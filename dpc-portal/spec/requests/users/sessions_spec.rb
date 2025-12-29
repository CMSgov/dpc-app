# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'Sessions', type: :request do
  include LoginSupport
  describe 'logout' do
    context 'logged in' do
      let!(:user) { create(:user) }
      before do
        sign_in user
      end
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

      it 'should redirect to login.gov' do
        delete '/users/sign_out'
        expect(response.location).to include(ENV.fetch('IDP_HOST'))
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
end
