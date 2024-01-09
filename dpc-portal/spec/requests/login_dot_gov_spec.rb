# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'LoginDotGov', type: :request do
  describe 'POST /users/auth/openid_connect' do
    it 'signs in a user' do
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:openid_connect,
                               { uid: '12345',
                                 info: { email: 'bob@example.com' },
                                 extra: { raw_info: { given_name: 'Bob',
                                                      family_name: 'Hoskins' } } })
      post '/users/auth/openid_connect'
      follow_redirect!
      expect(response.body).to include('Bob Hoskins')
    end
  end

  describe 'Get /users/auth/failure' do
    it 'succeeds' do
      get '/users/auth/failure'
      expect(response).to have_http_status(200)
    end
  end
end
