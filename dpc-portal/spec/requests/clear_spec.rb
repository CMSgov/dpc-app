# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'
require 'support/csp_auth_shared_examples'

RSpec.describe 'Clear', type: :request do
  let(:uuid) { SecureRandom.uuid }
  describe 'POST /auth/clear' do
    let!(:csp) { Csp.find_by(name: 'clear') || create(:csp, :clear) }
    let(:token) { 'bearer-token' }
    let(:id_token) { 'id-token' }
    let(:csp_auth_response) do
      { uid: uuid,
        credentials: { expires_in: 899,
                       token:,
                       id_token: },
        info: { email: 'bob2@example.com' },
        extra: { raw_info: { sub: uuid,
                             email: 'bob2@example.com',
                             given_name: 'Bob',
                             family_name: 'Hoskins',
                             SSN: '123456789',
                             ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
    end

    it_behaves_like 'a CSP client', :clear, '/auth/clear', expected_id_token: 'id-token'

    # IAL1 is no longer allowed should now be blocked entirely
    context 'IAL/1' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'bob@example.com',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
      end

      it 'returns 403 forbidden' do
        post '/auth/clear'
        follow_redirect!
        expect(response).to have_http_status(:forbidden)
      end

      it 'renders the clear_signin_fail error component' do
        post '/auth/clear'
        follow_redirect!
        expect(response.body).to include('CLEAR sign-in failed')
      end

      it 'logs the IAL1 blocked attempt' do
        allow(Rails.logger).to receive(:info)
        post '/auth/clear'
        follow_redirect!
        expect(Rails.logger).to have_received(:info).with(
          ['User attempted IAL1 login with CLEAR — not permitted',
           { actionContext: LoggingConstants::ActionContext::Authentication,
             actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }]
        )
      end

      it 'does not sign in the user' do
        post '/auth/clear'
        follow_redirect!
        expect(response).to be_forbidden
      end

      it 'does not set an authentication token' do
        post '/auth/clear'
        expect(request.session[:clear_token]).to be_nil
        expect(request.session[:clear_token_exp]).to be_nil
        expect(request.session[:clear_id_token]).to be_nil
      end

      context 'when a matching user account exists' do
        before do
          user = create(:user, given_name: 'Bob', family_name: 'Hoskins')
          create(:csp_user, user:, uuid:, csp:)
        end

        it 'still returns 403 forbidden' do
          post '/auth/clear'
          follow_redirect!
          expect(response).to have_http_status(:forbidden)
        end

        it 'does not sign in the user' do
          post '/auth/clear'
          follow_redirect!
          expect(response).to be_forbidden
        end
      end
    end

    context 'should add emails' do
      before do
        uuid = SecureRandom.uuid
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'email1@example.com',
                                                        given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

        user = create(:user)
        create(:csp_user, user:, uuid:, csp:)
      end

      it 'adds emails' do
        expect do
          post '/auth/clear'
          follow_redirect!
        end.to change { UserEmail.count }.by(1)

        email = UserEmail.last
        expect(email.email).to eq 'email1@example.com'
        expect(email.active).to eq true
        expect(email.primary).to eq true
      end
    end

    context 'should deactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'email1@example.com',
                                                        given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

        user = create(:user)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email@example.com', active: true)
      end

      it 'deactivates email' do
        post '/auth/clear'
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
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'email1@example.com',
                                                        given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

        user = create(:user)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email1@example.com', active: false, deactivated_at: 1.day.ago,
                            reactivated_at: nil)
      end

      it 'reactivates emails' do
        post '/auth/clear'
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
    let(:id_token) { 'id-token' }
    before do
      uuid = SecureRandom.uuid
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:clear,
                               { uid: uuid,
                                 credentials: { expires_in: 899,
                                                token: 'bearer-token',
                                                id_token: },
                                 info: { email: 'email1@example.com' },
                                 extra: { raw_info: { sub: uuid,
                                                      email: 'email1@example.com',
                                                      given_name: 'Bob',
                                                      family_name: 'Hoskins',
                                                      SSN: '123456789',
                                                      ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

      user = create(:user)
      csp = create(:csp, :clear)
      create(:csp_user, user:, uuid:, csp:)
      post '/auth/clear'
      follow_redirect!
    end
    it 'should redirect to CLEAR' do
      delete '/logout'
      expect(response.location).to include(ENV.fetch('CLEAR_IDP_HOST'))
      expect(response.location).to include("id_token_hint=#{id_token}")
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
      OmniAuth.config.add_mock(:clear,
                               { uid: uuid,
                                 info: { email: 'example1@example.com' },
                                 extra: { raw_info: { sub: uuid,
                                                      email: 'example1@example.com',
                                                      SSN: '123456789',
                                                      ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
    end
    it 'should show logout button' do
      get '/auth/no_account'
      expect(response.body).to include 'Sign out of CSP'
    end
  end

  describe 'CSP inactive' do
    let!(:csp) { Csp.find_by(name: 'clear') || create(:csp, :clear) }

    before do
      csp.end_date = DateTime.current - 1.year
      csp.save!

      user = create(:user)
      create(:csp_user, user:, uuid:, csp:)

      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(:clear,
                               { uid: uuid,
                                 credentials: { expires_in: 899,
                                                token: 'bearer-token',
                                                id_token: 'id-token' },
                                 info: { email: 'bob4@example.com' },
                                 extra: { raw_info: { sub: uuid,
                                                      email: 'bob4@example.com',
                                                      ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })
    end

    it 'should log error' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(
        ['User attempted to login with CLEAR but no active CSP found',
         { actionContext: LoggingConstants::ActionContext::Authentication,
           actionType: LoggingConstants::ActionType::InvalidCsp }]
      )
      post '/auth/clear'
      follow_redirect!
    end
  end
end
