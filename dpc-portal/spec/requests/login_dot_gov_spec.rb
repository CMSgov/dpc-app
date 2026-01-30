# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'LoginDotGov', type: :request do
  describe 'POST /auth/login_dot_gov' do
    RSpec.shared_examples 'an openid client' do
      context 'user exists' do
        before do
          user = create(:user, email: 'bob@example.com')
          create(:idp_uid, user:, uid: '12345', provider: 'login_dot_gov')
        end
        it 'should sign in a user' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(response).to be_redirect
          follow_redirect!
          expect(response).to be_ok
        end
        it 'should log on successful sign in' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(['User logged in',
                                                       { actionContext: LoggingConstants::ActionContext::Authentication,
                                                         actionType: LoggingConstants::ActionType::UserLoggedIn }])
          post '/auth/login_dot_gov'
          follow_redirect!
        end
        it 'should not add another user credential' do
          expect(IdpUid.where(uid: '12345', provider: 'login_dot_gov').count).to eq 1
          expect do
            post '/auth/login_dot_gov'
            follow_redirect!
          end.to change { IdpUid.count }.by(0)
        end
      end

      context 'user does not exist' do
        it 'should not persist user' do
          expect do
            post '/auth/login_dot_gov'
            follow_redirect!
          end.to change { User.count }.by(0)
        end
      end
    end

    let(:token) { 'bearer-token' }
    context 'IAL/2' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:login_dot_gov,
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

      it_behaves_like 'an openid client'

      context :user_exists do
        before do
          user = create(:user, email: 'bob@example.com')
          create(:idp_uid, user:, uid: '12345', provider: 'login_dot_gov')
        end
        it 'updates user names' do
          expect do
            post '/auth/login_dot_gov'
            follow_redirect!
          end.to change {
                   User.where(email: 'bob@example.com', given_name: 'Bob',
                              family_name: 'Hoskins').count
                 }.by 1
          expect(response.location).to eq organizations_url
        end

        it 'sets authentication token' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to eq token
          expect(request.session[:login_dot_gov_token_exp]).to_not be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_within(1.second).of 899.seconds.from_now
        end
      end

      context :user_does_not_exist do
        it 'does not sign in user' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(response).to be_redirect
          follow_redirect!
          expect(response).to be_redirect
        end

        it 'sets authentication token' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to eq token
          expect(request.session[:login_dot_gov_token_exp]).to_not be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_within(1.second).of 899.seconds.from_now
        end
      end
    end

    context 'IAL/1' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:login_dot_gov,
                                 { uid: '12345',
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { all_emails: %w[bob@example.com bob2@example.com],
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
      end

      it_behaves_like 'an openid client'

      context :user_exists do
        before do
          user = create(:user, email: 'bob@example.com', given_name: 'Bob',
                               family_name: 'Hoskins')
          create(:idp_uid, user:, uid: '12345', provider: 'login_dot_gov')
        end
        it 'does not update user names' do
          expect(User.where(email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(User.where(email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
        end

        it 'does not set authentication token' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_nil
        end
      end

      context 'user does not exist' do
        it 'does not sign in user' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(response.location).to eq no_account_url
          expect(response).to be_redirect
        end

        it 'should log' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['User logged in without account',
             { actionContext: LoggingConstants::ActionContext::Authentication,
               actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }]
          )
          post '/auth/login_dot_gov'
          follow_redirect!
        end

        it 'does not set authentication token' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(request.session[:login_dot_gov_token]).to be_nil
          expect(request.session[:login_dot_gov_token_exp]).to be_nil
        end
      end
    end
  end

  describe 'Get /auth/failure' do
    it 'should succeed' do
      get '/users/auth/failure'
      expect(response).to be_ok
    end

    it 'should log on failure' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['User cancelled login',
                                                   { actionContext: LoggingConstants::ActionContext::Authentication,
                                                     actionType: LoggingConstants::ActionType::UserCancelledLogin }])
      get '/users/auth/failure'
    end
  end

  describe 'Delete /logout' do
    it 'should redirect to login.gov' do
      delete '/logout'
      expect(response.location).to include(ENV.fetch('IDP_HOST'))
      expect(request.session[:user_return_to]).to be_nil
    end
    it 'should set return to invitation flow if invitation sent' do
      invitation = create(:invitation, :ao)
      delete "/logout?invitation_id=#{invitation.id}"
      expect(request.session[:user_return_to]).to eq organization_invitation_url(invitation.provider_organization.id,
                                                                                 invitation.id)
    end
  end

  describe 'Get /auth/no_account' do
    it 'should show logout button' do
      get '/auth/no_account'
      expect(response.body).to include 'Sign out of Login.gov'
    end
  end
end
