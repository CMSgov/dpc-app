# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Users::Sessions', type: :request do
  describe 'logout' do
    context 'logged in' do
      let!(:user) { create(:user) }
      before do
        sign_in user
      end
      it 'should prevent access' do
        delete '/users/sign_out'
        get '/organizations'
        expect(response).to redirect_to('/portal/users/sign_in')
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
  end
end
