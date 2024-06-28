# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'LoginDotGov', type: :request do
  describe 'POST /users/auth/openid_connect' do
    let(:token) { 'bearer-token' }
    context 'IAL/2' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:openid_connect,
                                 { uid: '12345',
                                   credentials: { expires_in: 899,
                                                  token: },
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        social_security_number: '1-2-3',
                                                        all_emails: %w[bob@example.com bob2@example.com],
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })
      end

      context :user_exists do
        before { create(:user, uid: '12345', provider: 'openid_connect', email: 'bob@example.com') }
        it 'signs in a user' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(response.location).to eq organizations_url
        end
        it 'does not add another user' do
          expect(User.where(uid: '12345', provider: 'openid_connect').count).to eq 1
          expect do
            post '/users/auth/openid_connect'
            follow_redirect!
          end.to change { User.count }.by(0)
        end
        it 'adds user names' do
          expect(User.where(uid: '12345', provider: 'openid_connect', email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 0
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(User.where(uid: '12345', provider: 'openid_connect', email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
        end

        it 'sets authentication token' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to eq token
          expect(request.session[:login_dot_gov_token_exp]).to_not be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_within(1.second).of 899.seconds.from_now
        end
      end
      context :user_does_not_exist do
        it 'does not persist user' do
          expect do
            post '/users/auth/openid_connect'
            follow_redirect!
          end.to change { User.count }.by(0)
        end
        it 'does not sign in user if not exist' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(response).to be_redirect
        end
      end

      it 'sets authentication token' do
        post '/users/auth/openid_connect'
        follow_redirect!
        expect(request.session[:login_dot_gov_token]).to eq token
        expect(request.session[:login_dot_gov_token_exp]).to_not be_nil
        expect(request.session[:login_dot_gov_token_exp]).to be_within(1.second).of 899.seconds.from_now
      end
    end

    context 'IAL/1' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:openid_connect,
                                 { uid: '12345',
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { all_emails: %w[bob@example.com bob2@example.com],
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
      end

      context :user_exists do
        before do
          create(:user, uid: '12345', provider: 'openid_connect', email: 'bob@example.com', given_name: 'Bob',
                        family_name: 'Hoskins')
        end
        it 'signs in a user' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(response.location).to eq organizations_url
        end

        it 'does not add another user' do
          expect do
            post '/users/auth/openid_connect'
            follow_redirect!
          end.to change { User.count }.by(0)
        end

        it 'does not update user names' do
          expect(User.where(uid: '12345', provider: 'openid_connect', email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(User.where(uid: '12345', provider: 'openid_connect', email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
        end

        it 'does not set authentication token' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_nil
        end
      end

      context 'user does not exist' do
        it 'does not sign in user' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(response.location).to eq new_user_session_url
          expect(flash[:alert]).to eq('You must have an account to sign in.')
          expect(response).to be_redirect
        end

        it 'does not persist user' do
          expect do
            post '/users/auth/openid_connect'
            follow_redirect!
          end.to change { User.count }.by(0)
        end

        it 'does not set authentication token' do
          post '/users/auth/openid_connect'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_nil
        end
      end
    end
  end
  describe 'Get /users/auth/failure' do
    it 'succeeds' do
      get '/users/auth/failure'
      expect(response).to have_http_status(200)
    end
  end
end
