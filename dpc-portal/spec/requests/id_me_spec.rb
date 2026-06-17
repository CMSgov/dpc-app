# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'

RSpec.describe 'IdMe', type: :request do
  let(:uuid) { SecureRandom.uuid }
  describe 'POST /auth/id_me' do
    let!(:csp) { Csp.find_by(name: 'id_me') || create(:csp, :id_me) }
    RSpec.shared_examples 'an id.me client' do
      context 'user exists' do
        before do
          user = create(:user, email: 'bob1@example.com', provider: :id_me)
          create(:csp_user, user:, uuid:, csp:)
        end
        it 'should sign in a user' do
          post '/auth/id_me'
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
                                                         csp: 'id_me' }])
          post '/auth/id_me'
          follow_redirect!
        end
        it 'should write a cookie with the last used csp' do
          post '/auth/id_me'
          follow_redirect!
          expect(cookies[:last_used_csp]).to eq 'id_me'
        end
        it 'should not add another user credential' do
          expect(CspUser.where(uuid:, csp:).count).to eq 1
          expect do
            post '/auth/id_me'
            follow_redirect!
          end.to change { CspUser.count }.by(0)
        end
      end

      context 'user does not exist' do
        it 'should not persist user' do
          expect do
            post '/auth/id_me'
            follow_redirect!
          end.to change { User.count }.by(0)
        end
      end
    end

    let(:token) { 'bearer-token' }
    context 'IAL/2' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:id_me,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token: },
                                   info: { email: 'bob2@example.com' },
                                   extra: { raw_info: { given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        social_security_number: '1-2-3',
                                                        emails_confirmed: %w[email1@example.com email2@example.com],
                                                        identity_assurance_level: 2 } } })
      end

      it_behaves_like 'an id.me client'

      context :user_exists do
        let(:db_user) { create(:user, uid: '12345', provider: 'id_me', email: 'bob@example.com') }
        before do
          create(:csp_user, user: db_user, uuid:, csp:)
        end
        it 'updates user names' do
          expect do
            post '/auth/id_me'
            follow_redirect!
          end.to change {
            User.where(id: db_user.id, given_name: 'Bob',
                       family_name: 'Hoskins').count
          }.by 1
          expect(response.location).to eq organizations_url
        end

        it 'sets authentication token' do
          post '/auth/id_me'
          follow_redirect!
          expect(request.session[:csp]).to eq 'id_me'
          expect(request.session[:id_me_token]).to eq token
          expect(request.session[:id_me_token_exp]).to_not be_nil
          expect(request.session[:id_me_token_exp]).to be_within(1.second).of 899.seconds.from_now
        end
      end

      context :user_does_not_exist do
        it 'does not sign in user' do
          post '/auth/id_me'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(response).to be_redirect
          follow_redirect!
          expect(response).to be_redirect
        end

        it 'sets authentication token' do
          post '/auth/id_me'
          follow_redirect!
          expect(request.session[:csp]).to eq 'id_me'
          expect(request.session[:id_me_token]).to eq token
          expect(request.session[:id_me_token_exp]).to_not be_nil
          expect(request.session[:id_me_token_exp]).to be_within(1.second).of 899.seconds.from_now
        end
      end
    end

    context 'IAL/1' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:id_me,
                                 { uid: uuid,
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { identity_assurance_level: 1 } } })
      end

      it_behaves_like 'an id.me client'

      context :user_exists do
        before do
          create(:user, provider: 'id_me', given_name: 'Bob', family_name: 'Hoskins', email: 'bob@example.com')
          create(:csp_user, user: User.last, uuid:, csp:)
        end
        it 'does not update user names' do
          expect(CspUser.where(uuid:).count).to eq 1
          expect(User.where(provider: 'id_me', email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
          post '/auth/id_me'
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(CspUser.where(uuid:, csp: csp).count).to eq 1
          db_user = CspUser.find_by(uuid:, csp: csp)&.user
          expect(db_user).to be_present
          expect(db_user.given_name).to eq 'Bob'
          expect(db_user.family_name).to eq 'Hoskins'
          expect(User.where(provider: 'id_me', email: 'bob@example.com', given_name: 'Bob',
                            family_name: 'Hoskins').count).to eq 1
        end

        it 'does not set authentication token' do
          post '/auth/id_me'
          follow_redirect!
          expect(request.session[:id_me_token]).to be_nil
          expect(request.session[:id_me_token_exp]).to be_nil
        end
      end

      context 'user does not exist' do
        it 'does not sign in user' do
          post '/auth/id_me'
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
          post '/auth/id_me'
          follow_redirect!
        end

        it 'does not set authentication token' do
          post '/auth/id_me'
          follow_redirect!
          expect(request.session[:id_me_token]).to be_nil
          expect(request.session[:id_me_token_exp]).to be_nil
        end
      end
    end

    context 'should add emails' do
      before do
        uuid = SecureRandom.uuid
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:id_me,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        social_security_number: '1-2-3',
                                                        emails_confirmed: %w[email1@example.com email2@example.com],
                                                        identity_assurance_level: 2 } } })

        user = create(:user, provider: :id_me)
        create(:csp_user, user:, uuid:, csp:)
      end

      it 'adds emails' do
        expect do
          post '/auth/id_me'
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
        OmniAuth.config.add_mock(:id_me,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        social_security_number: '1-2-3',
                                                        emails_confirmed: nil,
                                                        identity_assurance_level: 2 } } })

        user = create(:user, email: 'email1@example.com', provider: :id_me)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email@example.com', active: true)
      end

      it 'deactivates email' do
        post '/auth/id_me'
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
        OmniAuth.config.add_mock(:id_me,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        social_security_number: '1-2-3',
                                                        emails_confirmed: %w[email1@example.com],
                                                        identity_assurance_level: 2 } } })

        user = create(:user, email: 'email1@example.com', provider: :id_me)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email1@example.com', active: false, deactivated_at: 1.day.ago,
                            reactivated_at: nil)
      end

      it 'reactivates emails' do
        post '/auth/id_me'
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
      OmniAuth.config.add_mock(:id_me,
                               { uid: uuid,
                                 credentials: { expires_in: 899,
                                                token: 'bearer-token' },
                                 info: { email: 'email1@example.com' },
                                 extra: { raw_info: { given_name: 'Bob',
                                                      family_name: 'Hoskins',
                                                      social_security_number: '1-2-3',
                                                      emails_confirmed: %w[email1@example.com email2@example.com],
                                                      identity_assurance_level: 2 } } })

      user = create(:user, provider: :id_me)
      csp = create(:csp, :id_me)
      create(:csp_user, user:, uuid:, csp:)
      post '/auth/id_me'
      follow_redirect!
    end
    it 'should redirect to ID.me' do
      delete '/logout'
      expect(response.location).to include(ENV.fetch('IDP_ID_ME_HOST'))
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
      OmniAuth.config.add_mock(:id_me,
                               { uid: uuid,
                                 info: { email: 'example1@example.com' },
                                 extra: { raw_info: { emails_confirmed: %w[bob4@example.com bobby@example.com],
                                                      identity_assurance_level: 1 } } })
    end
    it 'should show logout button' do
      get '/auth/no_account'
      expect(response.body).to include 'Sign out of CSP'
    end
  end

  describe 'CSP inactive' do
    before do
      csp = Csp.find_by(name: 'id_me')
      csp.end_date = DateTime.current - 1.year
      csp.save!

      user = create(:user, email: 'bob5@example.com', provider: :id_me)
      create(:csp_user, user:, uuid:, csp:)

      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:id_me,
                               { uid: uuid,
                                 credentials: { expires_in: 899,
                                                token: 'bearer-token' },
                                 info: { email: 'bob4@example.com' },
                                 extra: { raw_info: { emails_confirmed: %w[bob4@example.com bobby@example.com],
                                                      identity_assurance_level: 2 } } })
    end

    it 'should log error' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(
        ['User attempted to login with ID.me but no active CSP found',
         { actionContext: LoggingConstants::ActionContext::Authentication,
           actionType: LoggingConstants::ActionType::InvalidCsp }]
      )
      post '/auth/id_me'
      follow_redirect!
    end
  end
end
