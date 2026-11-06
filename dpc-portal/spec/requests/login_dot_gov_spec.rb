# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'
require 'support/csp_auth_shared_examples'

RSpec.describe 'LoginDotGov', type: :request do
  auth_uid = SecureRandom.uuid
  let(:uuid) { auth_uid }

  it 'requests facial match verification from Login.gov' do
    expect(LOGIN_DOT_GOV_CLIENT_CONFIG[:acr_values])
      .to eq(CspIdentityAssurance::FACIAL_MATCH_REQUIRED)
  end

  describe 'POST /auth/login_dot_gov' do
    let!(:csp) { Csp.find_by(name: 'login_dot_gov') || create(:csp, :login_dot_gov) }
    let(:token) { 'bearer-token' }
    let(:id_token) { 'id-token' }
    let(:csp_auth_response) do
      { uid: uuid,
        credentials: { expires_in: 899,
                       token:,
                       id_token: },
        info: { email: 'bob@example.com' },
        extra: { raw_info: { given_name: 'Bob',
                             family_name: 'Hoskins',
                             social_security_number: '1-2-3',
                             all_emails: %w[bob2@example.com bobby@example.com],
                             ial: CspIdentityAssurance::FACIAL_MATCH_REQUIRED } } }
    end

    login_dot_gov_config = {
      provider: :login_dot_gov,
      auth_endpoint: '/auth/login_dot_gov',
      display_name: 'Login.gov',
      logout_host: ENV.fetch('IDP_LOGIN_DOT_GOV_HOST'),
      ial1_auth_response: {
        uid: auth_uid,
        info: { email: 'bob@example.com' },
        extra: { raw_info: { all_emails: %w[bob@example.com bob2@example.com],
                             ial: CspIdentityAssurance::IAL1 } }
      }
    }
    it_behaves_like 'a CSP client', login_dot_gov_config

    context 'should add emails' do
      before do
        uuid = SecureRandom.uuid
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: uuid,
            credentials: { expires_in: 899, token:, id_token: },
            info: { email: 'email1@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 social_security_number: '1-2-3',
                                 all_emails: %w[email1@example.com email2@example.com],
                                 ial: CspIdentityAssurance::FACIAL_MATCH_REQUIRED } } }
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

      it 'logs added emails' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['New user email created',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::AddNewUserEmail,
                                                                    csp: 'login_dot_gov')])
                                              .twice
        post '/auth/login_dot_gov'
        follow_redirect!
      end
    end

    context 'should deactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: uuid,
            credentials: { expires_in: 899, token:, id_token: },
            info: { email: 'email@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 social_security_number: '1-2-3',
                                 all_emails: nil,
                                 ial: CspIdentityAssurance::FACIAL_MATCH_REQUIRED } } }
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

      it 'logs deactivated email' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User email deactivated',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::DeactivateUserEmail,
                                                                    user_identifier: uuid,
                                                                    csp: 'login_dot_gov')])
        post '/auth/login_dot_gov'
        follow_redirect!
      end
    end

    context 'should reactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: uuid,
            credentials: { expires_in: 899, token:, id_token: },
            info: { email: 'email1@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 social_security_number: '1-2-3',
                                 all_emails: %w[email1@example.com],
                                 ial: CspIdentityAssurance::FACIAL_MATCH_REQUIRED } } }
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

      it 'logs reactivated email' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User email reactivated',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::ReactivateUserEmail,
                                                                    csp: 'login_dot_gov')])
        post '/auth/login_dot_gov'
        follow_redirect!
      end
    end

    context 'verified without facial match' do
      let(:no_facial_match_uuid) { SecureRandom.uuid }

      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: no_facial_match_uuid,
            credentials: { expires_in: 899, token:, id_token: },
            info: { email: 'nofacial@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 all_emails: %w[nofacial@example.com],
                                 ial: CspIdentityAssurance::VERIFIED } } }
        )
      end

      it 'returns 403 forbidden' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response).to have_http_status(:forbidden)
      end

      it 'renders the csp_signin_fail error component' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response.body).to include('Login.gov sign-in failed')
      end

      it 'does not set an authentication token' do
        post '/auth/login_dot_gov'
        follow_redirect!
        csp_session = CspSession.new(request.session)
        expect(csp_session.current).to be_nil
        expect(csp_session.token).to be_nil
        expect(csp_session.user).to be_nil
      end

      it 'does not create a CspUser' do
        expect do
          post '/auth/login_dot_gov'
          follow_redirect!
        end.to change { CspUser.count }.by(0)
      end
    end

    context 'user previously verified without facial match, has an existing account' do
      let(:existing_user) { create(:user, given_name: 'Bob', family_name: 'Hoskins') }
      let(:returning_uuid) { SecureRandom.uuid }

      before do
        returning_csp = Csp.find_by(name: 'login_dot_gov') || create(:csp, :login_dot_gov)
        csp_user = create(:csp_user, user: existing_user, uuid: returning_uuid, csp: returning_csp)
        create(:user_email, csp_user:, email: 'returning@example.com', primary: true, active: true)

        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: returning_uuid,
            credentials: { expires_in: 899, token:, id_token: },
            info: { email: 'returning@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 all_emails: %w[returning@example.com],
                                 ial: CspIdentityAssurance::VERIFIED } } }
        )
      end

      it 'still blocks the login despite the existing account' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response).to have_http_status(:forbidden)
      end

      it 'does not sign the existing user in' do
        post '/auth/login_dot_gov'
        follow_redirect!
        csp_session = CspSession.new(request.session)
        expect(csp_session.user).to be_nil
      end

      it 'does not update the existing user record' do
        existing_user.update!(given_name: 'Original')
        original_given_name = existing_user.given_name
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(existing_user.reload.given_name).to eq original_given_name
      end
    end

    context 'verified with facial match' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(
          :login_dot_gov,
          { uid: SecureRandom.uuid,
            credentials: { expires_in: 899, token:, id_token: },
            info: { email: 'verified@example.com' },
            extra: { raw_info: { given_name: 'Bob',
                                 family_name: 'Hoskins',
                                 all_emails: %w[verified@example.com],
                                 ial: CspIdentityAssurance::FACIAL_MATCH_REQUIRED } } }
        )
      end

      it 'is not blocked by the facial match guard' do
        post '/auth/login_dot_gov'
        follow_redirect!
        expect(response).not_to have_http_status(:forbidden)
      end
    end
  end
end
