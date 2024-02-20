# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'LoginDotGov', type: :request do
  describe 'POST /users/auth/openid_connect' do
    before do
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:openid_connect,
                               { uid: '12345',
                                 info: { email: 'bob@example.com' },
                                 extra: { raw_info: { given_name: 'Bob',
                                                      family_name: 'Hoskins',
                                                      social_security_number: '1-2-3' } } })
    end

    it 'signs in a user' do
      post '/users/auth/openid_connect'
      follow_redirect!
      expect(response.body).to include('Bob Hoskins')
    end

    it 'persists user if not exist' do
      expect(User.where(uid: '12345', provider: 'openid_connect').count).to eq 0
      expect do
        post '/users/auth/openid_connect'
        follow_redirect!
      end.to change { User.count }.by(1)
      expect(User.where(uid: '12345', provider: 'openid_connect', email: 'bob@example.com', given_name: 'Bob',
                        family_name: 'Hoskins').count).to eq 1
    end

    it 'does not persist user if exists' do
      create(:user, uid: '12345', provider: 'openid_connect', email: 'bob@example.com')
      expect(User.where(uid: '12345', provider: 'openid_connect').count).to eq 1
      expect do
        post '/users/auth/openid_connect'
        follow_redirect!
      end.to change { User.count }.by(0)
    end

    it 'saves hashed ssn in session' do
      post '/users/auth/openid_connect'
      follow_redirect!
      expect(cookies[:ao_id]).to_not be_blank
      jar = ActionDispatch::Cookies::CookieJar.build(request, cookies.to_hash)
      expect(jar.signed.encrypted[:ao_id]).to eq(Digest::SHA2.new(256).hexdigest('123'))
    end
  end

  describe 'Get /users/auth/failure' do
    it 'succeeds' do
      get '/users/auth/failure'
      expect(response).to have_http_status(200)
    end
  end
end
