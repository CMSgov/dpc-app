# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'AutoSessionLogoutService', type: :request do
  let(:user) { create(:user) }
  before { sign_in user }

  it 'is active' do
    get '/active'
    expect(response).to be_ok
  end

  it 'is timed out' do
    get '/timeout'
    expect(response.location).to include('/users/sign_in')
  end
end
