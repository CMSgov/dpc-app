# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'AutoSessionLogoutService', type: :request do
  include LoginSupport

  let(:user) { create_user_with_csp(csp: :login_dot_gov) }
  before { sign_in user, csp: :login_dot_gov }

  it 'is active' do
    get '/active'
    expect(response).to be_ok
  end

  it 'is timed out' do
    get '/timeout'
    expect(response).to redirect_to(sign_in_path)
  end
end
