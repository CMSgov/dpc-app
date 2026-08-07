# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'
require 'support/csp_auth_shared_examples'

RSpec.describe 'LoginDotGov', type: :request do
  auth_uid = SecureRandom.uuid
  let(:uuid) { auth_uid }

  describe 'POST /auth/login_dot_gov' do
    let!(:csp) { Csp.find_by(name: 'login_dot_gov') || create(:csp, :login_dot_gov) }
    let(:token) { 'bearer-token' }
    let(:csp_auth_response) do
      { uid: uuid,
        credentials: { expires_in: 899,
                       token: },
        info: { email: 'bob@example.com' },
        extra: { raw_info: { given_name: 'Bob',
                             family_name: 'Hoskins',
                             social_security_number: '1-2-3',
                             all_emails: %w[bob2@example.com bobby@example.com],
                             ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
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
                             ial: 'http://idmanagement.gov/ns/assurance/ial/1' } }
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

      it 'logs added emails' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['New user email created',
                                                     { actionContext: LoggingConstants::ActionContext::Authentication,
                                                       actionType: LoggingConstants::ActionType::AddNewUserEmail,
                                                       csp: 'login_dot_gov' }])
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
            credentials: { expires_in: 899, token: },
            info: { email: 'email@example.com' },
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

      it 'logs deactivated email' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User email deactivated',
                                                     { actionContext: LoggingConstants::ActionContext::Authentication,
                                                       actionType: LoggingConstants::ActionType::DeactivateUserEmail,
                                                       csp: 'login_dot_gov' }])
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

      it 'logs reactivated email' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User email reactivated',
                                                     { actionContext: LoggingConstants::ActionContext::Authentication,
                                                       actionType: LoggingConstants::ActionType::ReactivateUserEmail,
                                                       csp: 'login_dot_gov' }])
        post '/auth/login_dot_gov'
        follow_redirect!
      end
    end
  end
end
