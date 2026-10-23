# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'
require 'support/csp_auth_shared_examples'

RSpec.describe 'Clear', type: :request do
  let(:uuid) { SecureRandom.uuid }
  describe 'POST /auth/clear' do
    let!(:csp) { Csp.find_by(name: 'clear') || create(:csp, :clear) }
    # CLEAR's access token is a JWT carrying the verification_id used to fetch the verification session
    let(:verification_id) { 'verify_abc123' }
    let(:token) { JWT.encode({ 'verification_id' => verification_id }, nil, 'none') }
    let(:verification_session_url) { "#{CLEAR_CONFIG[:verification_session_endpoint]}/#{verification_id}" }
    let(:verification_session_body) { { status: 'success' } }
    let(:verification_session_response) do
      { status: 200, body: { id: verification_id }.merge(verification_session_body).to_json }
    end
    let(:id_token) { 'id-token' }

    def attempt_sign_in
      post '/auth/clear'
      follow_redirect!
    end

    before do
      stub_request(:get, verification_session_url)
        .with(headers: { 'Authorization' => "Bearer #{token}" })
        .to_return(verification_session_response)
    end

    let(:csp_auth_response) do
      { uid: uuid,
        credentials: { expires_in: 899,
                       token:,
                       id_token: },
        info: { email: 'bob@example.com' },
        extra: { raw_info: { sub: uuid,
                             email: 'bob@example.com',
                             given_name: 'Bob',
                             family_name: 'Hoskins',
                             SSN: '123456789',
                             ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
    end

    clear_config = {
      provider: :clear,
      auth_endpoint: '/auth/clear',
      display_name: 'CLEAR',
      logout_host: ENV.fetch('CLEAR_IDP_HOST'),
      expected_id_token: 'id-token' # covers store_id_token = true for clear_controller
    }
    it_behaves_like 'a CSP client', clear_config

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

      it 'logs added emails' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['New user email created',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::AddNewUserEmail,
                                                                    csp: 'clear')])
        post '/auth/clear'
        follow_redirect!
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
                                   info: { email: 'email@example.com' },
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

      it 'logs deactivated email' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User email deactivated',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::DeactivateUserEmail,
                                                                    user_identifier: uuid,
                                                                    csp: 'clear')])
        post '/auth/clear'
        follow_redirect!
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

      it 'logs reactivated email' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User email reactivated',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::ReactivateUserEmail,
                                                                    csp: 'clear')])
        post '/auth/clear'
        follow_redirect!
      end
    end

    context 'user cancels sign in' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: 'unknown',
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: nil },
                                   info: { email: 'Unknown' },
                                   extra: { raw_info: { sub: 'unknown',
                                                        email: 'Unknown',
                                                        given_name: 'Unknown',
                                                        family_name: 'Unknown' } } })
      end

      shared_examples 'does not sign in' do
        it 'does not store a token or sign the user in' do
          attempt_sign_in
          follow_redirect!
          csp_session = CspSession.new(request.session)
          expect(csp_session.token).to be_nil
          expect(csp_session.user).to be_nil
        end
      end

      context 'user cancels' do
        let(:verification_session_body) { { status: 'canceled' } }

        it 'redirects to the failure path with a cancelled message' do
          attempt_sign_in
          expect(response.location).to eq csp_failure_url(message: 'cancelled', strategy: 'clear')
        end

        it 'flashes the cancelled alert on the sign in page' do
          attempt_sign_in
          follow_redirect!
          expect(response.location).to eq sign_in_url
          expect(flash[:alert]).to eq CspErrorHandling::VERIFICATION_ALERT
        end

        it 'logs the cancellation' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(['User cancelled login',
                                                       hash_including(actionType: LoggingConstants::ActionType::UserCancelledLogin,
                                                                      csp: 'clear')])
          attempt_sign_in
          follow_redirect!
        end

        it_behaves_like 'does not sign in'
      end

      context 'verification fails' do
        let(:verification_session_body) { { status: 'fail', status_details: 'ip_blocked' } }

        it 'redirects to the failure path' do
          attempt_sign_in
          expect(response.location).to eq csp_failure_url(message: 'failed', strategy: 'clear')
        end

        it 'renders the verification failure page' do
          attempt_sign_in
          follow_redirect!
          expect(response).to be_ok
          expect(response.body).to include('Your identity could not be verified with CLEAR')
        end

        it 'logs the status and reason' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['CSP verification session was not successful',
             hash_including(actionType: LoggingConstants::ActionType::FailedLogin,
                            verificationReason: 'fail:ip_blocked', csp: 'clear')]
          )
          attempt_sign_in
        end

        it_behaves_like 'does not sign in'
      end

      context 'verification session expired' do
        let(:verification_session_body) { { status: 'expired' } }

        it 'redirects to the failure path with an expired message' do
          attempt_sign_in
          expect(response.location).to eq csp_failure_url(message: 'expired', strategy: 'clear')
        end

        it 'flashes the expired verification alert on the sign in page' do
          attempt_sign_in
          follow_redirect!
          expect(response.location).to eq sign_in_url
          expect(flash[:alert]).to eq CspErrorHandling::SESSION_EXPIRED
        end

        it 'logs the expiry' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['User verification session expired',
             hash_including(actionType: LoggingConstants::ActionType::FailedLogin, csp: 'clear')]
          )
          attempt_sign_in
          follow_redirect!
        end

        it_behaves_like 'does not sign in'
      end

      context 'session was cancelled for a reason other than the user choosing to' do
        let(:verification_session_body) { { status: 'canceled', status_details: 'ineligible' } }

        it 'is reported as a failure, not a cancellation' do
          attempt_sign_in
          expect(response.location).to eq csp_failure_url(message: 'failed', strategy: 'clear')
        end
      end

      context 'verification is not finished' do
        let(:verification_session_body) { { status: 'awaiting_manual_review' } }

        it 'is treated as unavailable' do
          attempt_sign_in
          expect(response.location).to eq csp_failure_url(message: 'unavailable', strategy: 'clear')
        end

        it_behaves_like 'does not sign in'
      end
    end

    context 'Get Verification Session cannot be completed' do
      before do
        user = create(:user)
        create(:csp_user, user:, uuid:, csp:)
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear, csp_auth_response)
      end

      shared_examples 'fails closed' do
        it 'redirects to the failure path with an unavailable message' do
          attempt_sign_in
          expect(response.location).to eq csp_failure_url(message: 'unavailable', strategy: 'clear')
        end

        it 'flashes the unavailable alert on the sign in page' do
          attempt_sign_in
          follow_redirect!
          expect(response.location).to eq sign_in_url
          expect(flash[:alert]).to eq CspErrorHandling::EXTERNAL_SYSTEM_ERROR
        end

        it 'does not sign the user in even though CLEAR returned an id_token' do
          attempt_sign_in
          follow_redirect!
          csp_session = CspSession.new(request.session)
          expect(csp_session.token).to be_nil
          expect(csp_session.user).to be_nil
        end

        it 'logs the error' do
          allow(Rails.logger).to receive(:error)
          expect(Rails.logger).to receive(:error).with(
            ['CSP verification session could not be retrieved',
             hash_including(actionType: LoggingConstants::ActionType::CspUnavailable, csp: 'clear')]
          )
          attempt_sign_in
        end
      end

      context 'CLEAR returns a server error even though we hold a success for this session' do
        let(:verification_session_response) { { status: 500, body: '' } }

        before { create(:csp_user_verification, csp:, csp_verification_id: verification_id, status: 'success') }

        it_behaves_like 'fails closed'
      end

      context 'CLEAR returns a server error' do
        let(:verification_session_response) { { status: 500, body: 'oops' } }
        it_behaves_like 'fails closed'
      end

      context 'CLEAR rejects the access token' do
        let(:verification_session_response) { { status: 401, body: '{}' } }
        it_behaves_like 'fails closed'
      end

      context 'CLEAR times out' do
        before { stub_request(:get, verification_session_url).to_timeout }
        it_behaves_like 'fails closed'
      end

      context 'CLEAR returns something unparseable' do
        let(:verification_session_response) { { status: 200, body: '<html>' } }
        it_behaves_like 'fails closed'
      end

      context 'access token has no verification_id' do
        let(:token) { JWT.encode({ 'sub' => 'x' }, nil, 'none') }
        it_behaves_like 'fails closed'

        it 'does not call CLEAR' do
          attempt_sign_in
          expect(a_request(:get, /verification_sessions/)).not_to have_been_made
        end
      end

      context 'access token is not a JWT' do
        let(:token) { 'bearer-token' }
        it_behaves_like 'fails closed'
      end
    end

    context 'recording the verification' do
      let(:session_created_at) { 2.minutes.ago.to_i }
      let(:completed_at) { Time.current.to_i }
      let(:verification_session_body) do
        { status: 'success', created_at: session_created_at, completed_at:,
          checks: [{ name: 'Gov ID is not expired', value: true, status: 'completed' }] }
      end
      let(:no_id_token_response) do
        { uid: uuid, credentials: { expires_in: 899, token:, id_token: nil }, info: { email: 'Unknown' },
          extra: { raw_info: { sub: uuid, email: 'Unknown', given_name: 'Unknown', family_name: 'Unknown' } } }
      end

      before { OmniAuth.config.test_mode = true }

      context 'an existing user who passes' do
        let!(:user) { create(:user) }

        before do
          create(:csp_user, user:, uuid:, csp:)
          OmniAuth.config.add_mock(:clear, csp_auth_response)
        end

        it 'stores the session linked to the user, with its times and checks' do
          expect { attempt_sign_in }.to change { CspUserVerification.count }.by(1)
          verification = CspUserVerification.last
          expect(verification).to have_attributes(csp:, csp_verification_id: verification_id, status: 'success',
                                                  user:, invitation: nil,
                                                  session_created_at: Time.zone.at(session_created_at),
                                                  completed_at: Time.zone.at(completed_at))
          expect(verification.csp_verification_checks.pluck(:check_name)).to eq ['Gov ID is not expired']
        end

        it 'keeps each response as its own row on a repeat login' do
          allow(Rails.logger).to receive(:error)
          attempt_sign_in
          expect { attempt_sign_in }.to change { CspUserVerification.count }.by(1)
          expect(CspUserVerification.where(csp_verification_id: verification_id).count).to eq 2
          expect(Rails.logger).not_to have_received(:error).with(['Unable to record CSP verification', anything])
        end

        it 'still signs the user in' do
          attempt_sign_in
          expect(response.location).to eq organizations_url
        end
      end

      context 'a new user registering with an invitation' do
        let(:invitation) { create(:invitation, :ao) }

        before do
          OmniAuth.config.add_mock(:clear, csp_auth_response)
          allow_any_instance_of(ClearController).to receive(:invitation_match).and_return(invitation)
        end

        it 'stores the session linked to the invitation, not a user' do
          attempt_sign_in
          expect(CspUserVerification.last).to have_attributes(invitation:, user: nil, status: 'success')
        end
      end

      context 'a failure for an existing user' do
        let!(:user) { create(:user) }
        let(:verification_session_body) { { status: 'fail', status_details: 'ip_blocked' } }

        before do
          create(:csp_user, user:, uuid:, csp:)
          OmniAuth.config.add_mock(:clear, no_id_token_response)
        end

        it 'is stored, linked to the user' do
          attempt_sign_in
          expect(CspUserVerification.last).to have_attributes(status: 'fail', user:)
        end
      end

      context 'a failure for a new user with an invitation' do
        let(:invitation) { create(:invitation, :ao) }
        let(:verification_session_body) { { status: 'fail', status_details: 'authentication_error' } }

        before do
          OmniAuth.config.add_mock(:clear, no_id_token_response)
          allow_any_instance_of(ClearController).to receive(:invitation_match).and_return(invitation)
        end

        it 'is stored linked to the invitation' do
          attempt_sign_in
          expect(CspUserVerification.last).to have_attributes(status: 'fail', invitation:, user: nil)
        end
      end

      context 'a cancellation by someone we cannot identify' do
        let(:verification_session_body) { { status: 'canceled' } }

        before { OmniAuth.config.add_mock(:clear, no_id_token_response.merge(uid: 'unknown')) }

        it 'is stored with no user or invitation' do
          expect { attempt_sign_in }.to change { CspUserVerification.count }.by(1)
          expect(CspUserVerification.last).to have_attributes(status: 'canceled', user: nil, invitation: nil)
        end
      end

      context 'when storing fails' do
        before do
          user = create(:user)
          create(:csp_user, user:, uuid:, csp:)
          OmniAuth.config.add_mock(:clear, csp_auth_response)
          allow(CspUserVerification).to receive(:record!).and_raise(ActiveRecord::StatementInvalid, 'boom')
        end

        it 'does not block the login' do
          attempt_sign_in
          expect(response.location).to eq organizations_url
        end

        it 'logs the error' do
          allow(Rails.logger).to receive(:error)
          expect(Rails.logger).to receive(:error).with(
            ['Unable to record CSP verification',
             hash_including(actionType: LoggingConstants::ActionType::CspVerificationNotRecorded, csp: 'clear')]
          )
          attempt_sign_in
        end
      end
    end

    context 'successful verification' do
      let(:verification_session_body) { { status: 'success' } }

      it 'calls Get Verification Session with the access token' do
        user = create(:user)
        create(:csp_user, user:, uuid:, csp:)
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear, csp_auth_response)
        post '/auth/clear'
        follow_redirect!
        expect(a_request(:get, verification_session_url)
                 .with(headers: { 'Authorization' => "Bearer #{token}" })).to have_been_made.once
        expect(response.location).to eq organizations_url
      end
    end
  end
end
