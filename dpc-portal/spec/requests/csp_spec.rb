# frozen_string_literal: true

require 'rails_helper'

# These responses are the same across all CSPs.
RSpec.describe 'CSP', type: :request do
  describe 'Get /auth/failure' do
    let(:auth_failure_path) { '/auth/failure?message=access_denied&strategy=csp' }
    it 'should redirect to sign-in' do
      get auth_failure_path
      expect(response.location).to eq(sign_in_url)
    end

    it 'should log on failure' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['User cancelled login',
                                                   hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                  actionType: LoggingConstants::ActionType::UserCancelledLogin,
                                                                  csp: 'csp',
                                                                  timestamp: a_kind_of(String))])
      get auth_failure_path
    end
  end

  describe 'Get /auth/no_account' do
    it 'should show logout button' do
      get '/auth/no_account'
      expect(response.body).to include 'Back to sign in'
    end
  end
end
