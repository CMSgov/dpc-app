# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'

RSpec.describe 'LoginDotGov', type: :request do
  let(:uuid) { SecureRandom.uuid }
  describe 'POST /auth/login_dot_gov' do
    let!(:csp) { Csp.find_by(name: 'login_dot_gov') || create(:csp, :login_dot_gov) }
    RSpec.shared_examples 'a login.gov client' do
      context 'user exists' do
        before do
          user = create(:user)
          create(:csp_user, user:, uuid:, csp:)
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
                                                         actionType: LoggingConstants::ActionType::UserLoggedIn,
                                                         csp: 'login_dot_gov' }])
          post '/auth/login_dot_gov'
          follow_redirect!
        end
        it 'should write a cookie with the last used csp' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(cookies[:last_used_csp]).to eq 'login_dot_gov'
        end
        it 'should not add another user credential' do
          expect(CspUser.where(uuid:, csp:).count).to eq 1
          expect do
            post '/auth/login_dot_gov'
            follow_redirect!
          end.to change { CspUser.count }.by(0)
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
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token: },
                                   info: { email: 'bob2@example.com' },
                                   extra: { raw_info: { given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        social_security_number: '1-2-3',
                                                        all_emails: %w[bob2@example.com bobby@example.com],
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })
      end

      it_behaves_like 'a login.gov client'

      context :user_exists do
        let(:db_user) { create(:user) }
        before { create(:csp_user, user: db_user, uuid:, csp:) }

        it 'updates user names' do
          expect do
            post '/auth/login_dot_gov'
            follow_redirect!
          end.to change {
            User.where(id: db_user.id, given_name: 'Bob', family_name: 'Hoskins').count
          }.by 1
          expect(response.location).to eq organizations_url
        end

        it 'sets authentication token' do
          post '/auth/login_dot_gov'
          follow_redirect!

          csp_session = CspSession.new(request.session)
          expect(csp_session.current).to eq 'login_dot_gov'
          expect(csp_session.token).to eq token
          expect(csp_session.token_exp).to_not be_nil
          expect(csp_session.token_exp).to be_within(1.second).of 899.seconds.from_now
          expect(csp_session.id_token).to be_nil
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
          csp_session = CspSession.new(request.session)
          expect(csp_session.current).to eq 'login_dot_gov'
          expect(csp_session.token).to eq token
          expect(csp_session.token_exp).to_not be_nil
          expect(csp_session.token_exp).to be_within(1.second).of 899.seconds.from_now
          expect(csp_session.user).to be_nil
          expect(csp_session.id_token).to be_nil
        end
      end
    end

    # IAL1 is no longer allowed should now be blocked entirely
    context 'IAL/1' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:login_dot_gov,
                                 { uid: uuid,
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { all_emails: %w[bob@example.com bob2@example.com],
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
      end

      it 'returns 403 forbidden' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response).to have_http_status(:forbidden)
      end

      it 'renders the login_gov_signin_fail error component' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response.body).to include('Login.gov sign-in failed')
      end

      it 'logs the IAL1 blocked attempt' do
        allow(Rails.logger).to receive(:info)
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(Rails.logger).to have_received(:info).with(
          ['User attempted IAL1 login with Login.gov — not permitted',
           { actionContext: LoggingConstants::ActionContext::Authentication,
             actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }]
        )
      end

      it 'does not sign in the user' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response).to be_forbidden
      end

      it 'does not set an authentication token' do
        post '/auth/login_dot_gov'
        csp_session = CspSession.new(request.session)
        expect(csp_session.current).to be_nil
        expect(csp_session.token).to be_nil
        expect(csp_session.token_exp).to be_nil
        expect(csp_session.id_token).to be_nil
        expect(csp_session.user).to be_nil
      end

      context 'when a matching user account exists' do
        before do
          user = create(:user, given_name: 'Bob', family_name: 'Hoskins')
          create(:csp_user, user:, uuid:, csp:)
        end

        it 'still returns 403 forbidden' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(response).to have_http_status(:forbidden)
        end

        it 'does not sign in the user' do
          post '/auth/login_dot_gov'
          follow_redirect!
          expect(response).to be_forbidden
        end
      end
    end

    context 'should add emails' do
      before do
        uuid = SecureRandom.uuid
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: uuid,
            credentials: { expires_in: 899, token: },
            info: { email: 'email1@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 social_security_number: '1-2-3',
                                 all_emails: %w[email1@example.com email2@example.com],
                                 ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
        )
        user = create(:user)
        create(:csp_user, user:, uuid:, csp:)
      end

      it 'adds emails' do
        expect do
          post '/auth/login_dot_gov'
          follow_redirect!
        end.to change { UserEmail.count }.by(2)

        emails = UserEmail.last(2).pluck(:email)
        expect(emails).to match_array(%w[email1@example.com email2@example.com])
        expect(UserEmail.pluck(:active)).to all(be true)
        expect(UserEmail.find(&:primary?).email).to eq 'email1@example.com'
        expect(UserEmail.count(&:primary?)).to eq 1
      end
    end

    context 'should deactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: uuid,
            credentials: { expires_in: 899, token: },
            info: { email: 'email1@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 social_security_number: '1-2-3',
                                 all_emails: nil,
                                 ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
        )
        user = create(:user)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email@example.com', active: true)
      end

      it 'deactivates email' do
        post '/auth/login_dot_gov'
        follow_redirect!

        email = UserEmail.find_by(csp_user: CspUser.last, email: 'email@example.com')
        expect(email.active).to eq false
        expect(email.deactivated_at).to_not be_nil
        expect(email.reactivated_at).to be_nil
      end
    end

    context 'should reactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: uuid,
            credentials: { expires_in: 899, token: },
            info: { email: 'email1@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 social_security_number: '1-2-3',
                                 all_emails: %w[email1@example.com],
                                 ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
        )
        user = create(:user)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email1@example.com', active: false,
                            deactivated_at: 1.day.ago, reactivated_at: nil)
      end

      it 'reactivates emails' do
        post '/auth/login_dot_gov'
        follow_redirect!

        email = UserEmail.find_by(csp_user: CspUser.last, email: 'email1@example.com')
        expect(email.active).to eq true
        expect(email.deactivated_at).to be_nil
        expect(email.reactivated_at).to_not be_nil
        expect(email.primary).to eq true
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
    before do
      uuid = SecureRandom.uuid
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:login_dot_gov,
                               { uid: uuid,
                                 credentials: { expires_in: 899,
                                                token: 'bearer-token' },
                                 info: { email: 'email1@example.com' },
                                 extra: { raw_info: { given_name: 'Bob',
                                                      family_name: 'Hoskins',
                                                      social_security_number: '1-2-3',
                                                      all_emails: %w[email1@example.com email2@example.com],
                                                      ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

      user = create(:user)
      csp = create(:csp, :login_dot_gov)
      create(:csp_user, user:, uuid:, csp:)
      post '/auth/login_dot_gov'
      follow_redirect!
    end
    it 'should redirect to login.gov' do
      delete '/logout'
      expect(response.location).to include(ENV.fetch('IDP_LOGIN_DOT_GOV_HOST'))
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
    before do
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:login_dot_gov,
                               { uid: uuid,
                                 info: { email: 'example1@example.com' },
                                 extra: { raw_info: { all_emails: %w[bob4@example.com bobby@example.com],
                                                      ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
    end
    it 'should show logout button' do
      get '/auth/no_account'
      expect(response.body).to include 'Sign out of CSP'
    end
  end

  describe 'CSP inactive' do
    let!(:csp) { Csp.find_by(name: 'login_dot_gov') || create(:csp, :login_dot_gov) }
    before do
      csp.end_date = DateTime.current - 1.year
      csp.save!

      user = create(:user)
      create(:csp_user, user:, uuid:, csp:)

      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:login_dot_gov,
                               { uid: uuid,
                                 credentials: { expires_in: 899,
                                                token: 'bearer-token' },
                                 info: { email: 'bob4@example.com' },
                                 extra: { raw_info: { all_emails: %w[bob4@example.com bobby@example.com],
                                                      ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })
    end

    it 'should log error' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(
        ['User attempted to login with Login.gov but no active CSP found',
         { actionContext: LoggingConstants::ActionContext::Authentication,
           actionType: LoggingConstants::ActionType::InvalidCsp }]
      )
      post '/auth/login_dot_gov'
      follow_redirect!
    end
  end
end
