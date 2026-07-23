# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'AutoSessionLogoutService', type: :request do
  include LoginSupport

  LoginSupport::CSP_MAP.each do |provider, display_name|
    context "using #{display_name}" do
      let(:user) { create_user_with_csp(csp: provider) }
      before { sign_in user, csp: provider }

      it 'is active' do
        get '/active'
        expect(response).to be_ok
      end

      it 'is timed out' do
        get '/timeout'
        expect(response).to redirect_to(sign_in_path)
      end
    end
  end
end
