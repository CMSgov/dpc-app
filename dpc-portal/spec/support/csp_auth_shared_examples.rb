# frozen_string_literal: true

RSpec.shared_examples 'a CSP client' do |config|
  provider = config[:provider]
  auth_endpoint = config[:auth_endpoint]
  display_name = config[:display_name]
  logout_host = config[:logout_host]
  expected_id_token = config[:expected_id_token]
  ial1_auth_response = config[:ial1_auth_response]
  csp_name = provider.to_s

  context 'IAL/2' do
    before do
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(provider, csp_auth_response)
    end

    context 'user exists' do
      let!(:user) { create(:user) }
      before do
        csp = Csp.find_by(name: csp_name) || create(:csp, name: csp_name)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'bob@example.com', primary: true, active: true)
      end

      it 'should sign in a user' do
        post auth_endpoint
        follow_redirect!
        expect(response.location).to eq organizations_url
        expect(response).to be_redirect
        follow_redirect!
        expect(response).to be_ok
      end

      it 'should log on successful sign in' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User logged in',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::UserLoggedIn,
                                                                    user_identifier: uuid,
                                                                    csp: csp_name)])
        post auth_endpoint
        follow_redirect!
      end

      it 'should write a cookie with the last used csp' do
        post auth_endpoint
        follow_redirect!
        expect(cookies[:last_used_csp]).to eq csp_name
      end

      it 'should not add another user credential' do
        expect(CspUser.where(uuid:, csp:).count).to eq 1
        expect do
          post auth_endpoint
          follow_redirect!
        end.to change { CspUser.count }.by(0)
      end

      it 'updates user names' do
        expect do
          post auth_endpoint
          follow_redirect!
        end.to change {
          User.where(id: user.id, given_name: 'Bob', family_name: 'Hoskins').count
        }.by 1
        expect(response.location).to eq organizations_url
      end

      it 'sets authentication token' do
        post auth_endpoint
        follow_redirect!

        csp_session = CspSession.new(request.session)
        expect(csp_session.current).to eq csp_name
        expect(csp_session.token).to eq token
        expect(csp_session.token_exp).to_not be_nil
        expect(csp_session.token_exp).to be_within(1.second).of 899.seconds.from_now
        expect(csp_session.id_token).to eq expected_id_token
      end
    end

    context 'user exists with different email' do
      before do
        user = create(:user)
        csp = Csp.find_by(name: csp_name) || create(:csp, name: csp_name)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'original@example.com', active: true)
      end

      it 'renders the add email component' do
        post auth_endpoint
        follow_redirect!
        expect(response).to be_ok
        expect(response.body).to include('Existing account found')
        expect(response.body).to include(EmailMask.masked('original@example.com'))
        expect(response.body).to include('Link to existing account')
        expect(response.body).to include('Start over')
      end

      it 'logs about existing account' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User has existing account associated with different email',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::MergeUserAccountEmail,
                                                                    csp: csp_name)])
        post auth_endpoint
        follow_redirect!
      end
    end

    context 'user does not exist' do
      it 'should not persist user' do
        expect do
          post auth_endpoint
          follow_redirect!
        end.to change { User.count }.by(0)
      end

      it 'does not sign in user' do
        post auth_endpoint
        follow_redirect!
        expect(response).to be_redirect
        expect(response.location).to eq organizations_url
        follow_redirect!
        expect(response.body).to include('The email you used to sign in was not recognized')
        expect(response.body).to include('Back to sign in')
      end

      it 'sets authentication token' do
        post auth_endpoint
        follow_redirect!
        csp_session = CspSession.new(request.session)
        expect(csp_session.current).to eq csp_name
        expect(csp_session.token).to eq token
        expect(csp_session.token_exp).to_not be_nil
        expect(csp_session.token_exp).to be_within(1.second).of 899.seconds.from_now
        expect(csp_session.id_token).to eq expected_id_token
        expect(csp_session.user).to be_nil
      end
    end
  end

  # Login.gov and ID.me allow users to request assurance level based on input parameters
  # IAL1 is no longer allowed across dpc-portal and should now be blocked
  if ial1_auth_response
    context 'IAL/1' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(provider, ial1_auth_response)
      end

      it 'returns 403 forbidden' do
        post auth_endpoint
        follow_redirect!
        expect(response).to have_http_status(:forbidden)
      end

      it 'renders the csp_signin_fail error component' do
        post auth_endpoint
        follow_redirect!
        expect(response.body).to include("#{display_name} sign-in failed")
      end

      it 'logs the IAL1 blocked attempt' do
        allow(Rails.logger).to receive(:info)
        post auth_endpoint
        follow_redirect!
        expect(Rails.logger).to have_received(:info).with(
          ["User attempted IAL1 login with #{display_name} — not permitted",
           hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                          actionType: LoggingConstants::ActionType::UserLoginWithoutAccount)]
        )
      end

      it 'does not sign in the user' do
        post auth_endpoint
        follow_redirect!
        expect(response).to be_forbidden
      end

      it 'does not set an authentication token' do
        post auth_endpoint
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
          # create(:csp_user, user:, uuid: ial1_auth_response[:uid], csp:)
        end

        it 'still returns 403 forbidden' do
          post auth_endpoint
          follow_redirect!
          expect(response).to have_http_status(:forbidden)
        end

        it 'does not sign in the user' do
          post auth_endpoint
          follow_redirect!
          expect(response).to be_forbidden
        end
      end
    end
  end

  describe 'CSP inactive' do
    before do
      csp.end_date = DateTime.current - 1.year
      csp.save!

      user = create(:user)
      create(:csp_user, user:, uuid:, csp:)

      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(provider, csp_auth_response)
    end

    it 'should log error' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(
        ["User attempted to login with #{display_name} but no active CSP found",
         hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                        actionType: LoggingConstants::ActionType::InvalidCsp)]
      )
      post auth_endpoint
      follow_redirect!
    end
  end

  describe 'API errors' do
    context "when #{display_name} returns 500 server error" do
      let(:error) { :server_error }
      let(:attempt_sign_in) do
        post auth_endpoint
        follow_redirect!
        expect(response.location).to eq("/auth/failure?message=#{error}&strategy=#{csp_name}")
        follow_redirect!
      end
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.mock_auth[provider] = error
      end

      it 'returns 503 service unavailable' do
        attempt_sign_in
        expect(response).to have_http_status(:service_unavailable)
      end

      it 'renders the server error component' do
        attempt_sign_in
        expect(response.body).to include(I18n.t('verification.server_error_status'))
      end

      it 'does not sign in the user' do
        attempt_sign_in
        csp_session = CspSession.new(request.session)
        expect(csp_session.user).to be_nil
        expect(csp_session.token).to be_nil
      end

      it 'does not create a CspUser' do
        expect do
          attempt_sign_in
        end.to change { CspUser.count }.by(0)
      end

      it 'logs the CSP authentication error' do
        allow(Rails.logger).to receive(:error)
        expect(Rails.logger).to receive(:error).with(
          ['CSP Authentication error',
           hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                          actionType: LoggingConstants::ActionType::CspUnavailable,
                          error: error.to_s,
                          csp: csp_name,
                          timestamp: a_kind_of(String))]
        )
        attempt_sign_in
      end
    end
    context "when #{display_name} returns 400 invalid argument" do
      let(:error) { :bad_request }
      let(:attempt_sign_in) do
        post auth_endpoint
        follow_redirect!
        expect(response.location).to eq("/auth/failure?message=#{error}&strategy=#{csp_name}")
        follow_redirect!
      end
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.mock_auth[provider] = error
      end

      it 'does not return 503 service unavailable' do
        attempt_sign_in
        expect(response).to be_ok
      end

      it 'renders the CSP sign-in fail component' do
        attempt_sign_in
        expect(response.body).not_to include(I18n.t('verification.server_error_status'))
        expect(response.body).to include(I18n.t('verification.csp_signin_fail_status', csp_display_name: display_name))
        expect(response.body).to include(I18n.t('verification.csp_signin_fail_text', csp_display_name: display_name))
      end

      it 'does not sign in the user' do
        attempt_sign_in
        csp_session = CspSession.new(request.session)
        expect(csp_session.user).to be_nil
        expect(csp_session.token).to be_nil
      end

      it 'does not create a CspUser' do
        expect do
          attempt_sign_in
        end.to change { CspUser.count }.by(0)
      end

      it 'logs the CSP authentication error' do
        allow(Rails.logger).to receive(:error)
        expect(Rails.logger).to receive(:error).with(
          ['CSP Configuration error',
           hash_including(actionContext: LoggingConstants::ActionContext::Registration,
                          actionType: LoggingConstants::ActionType::FailedLogin,
                          csp: csp_name,
                          timestamp: a_kind_of(String))]
        )
        attempt_sign_in
      end
    end

    context "when #{display_name} returns 403 access denied" do
      let(:error) { :access_denied }
      let(:attempt_sign_in) do
        post auth_endpoint
        follow_redirect!
        expect(response.location).to eq("/auth/failure?message=#{error}&strategy=#{csp_name}")
        follow_redirect!
      end
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.mock_auth[provider] = error
      end

      it 'does not return 503 service unavailable' do
        attempt_sign_in
        expect(response).to be_ok
      end

      it 'renders the CSP sign-in cancel component' do
        attempt_sign_in
        expect(response.body).not_to include(I18n.t('verification.server_error_status'))
        expect(response.body).to include(I18n.t('verification.csp_signin_cancel_status',
                                                csp_display_name: display_name))
        expect(response.body).to include(I18n.t('verification.csp_signin_cancel_text', csp_display_name: display_name))
      end

      it 'does not sign in the user' do
        attempt_sign_in
        csp_session = CspSession.new(request.session)
        expect(csp_session.user).to be_nil
        expect(csp_session.token).to be_nil
      end

      it 'does not create a CspUser' do
        expect do
          attempt_sign_in
        end.to change { CspUser.count }.by(0)
      end

      it 'logs the CSP authentication error' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(
          ['User cancelled login',
           hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                          actionType: LoggingConstants::ActionType::UserCancelledLogin,
                          csp: csp_name,
                          timestamp: a_kind_of(String))]
        )
        attempt_sign_in
      end
    end
  end

  describe 'Delete /logout' do
    before do
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(provider, csp_auth_response)

      user = create(:user)
      csp_user = create(:csp_user, user:, uuid:, csp:)
      create(:user_email, csp_user:, email: 'bob@example.com', primary: true, active: true)
      post auth_endpoint
      follow_redirect!
    end

    it "should redirect to #{display_name}" do
      delete '/logout'
      expect(response.location).to include(logout_host)
      # id_token_hint for CLEAR csp only
      expect(response.location).to include("id_token_hint=#{expected_id_token}") if expected_id_token
      expect(request.session[:user_return_to]).to be_nil
    end

    it 'should set return to invitation flow if invitation sent' do
      invitation = create(:invitation, :ao)
      delete "/logout?invitation_id=#{invitation.id}"
      expect(request.session[:user_return_to]).to eq organization_invitation_url(invitation.provider_organization.id,
                                                                                 invitation.id)
    end
  end

  describe 'Post /update' do
    context 'when user is not authorized' do
      let(:user) { create(:user) }
      let(:csp_user) { create(:csp_user, user:, uuid:, csp:) }
      let(:params) do
        { id: csp_user.id, csp: csp.id, all_emails: %w[alice1@example.com alice2@example.com],
          primary_email: 'alice1@example.com' }
      end

      context 'not logged in' do
        it 'returns forbidden' do
          post '/update', params: params
          expect(response.status).to eq 403
        end

        it 'does not add emails' do
          expect do
            post '/update', params: params
          end.to change { UserEmail.count }.by(0)
        end
      end

      context 'different user logged in' do
        before do
          OmniAuth.config.test_mode = true
          OmniAuth.config.add_mock(provider, csp_auth_response)

          other_user = create(:user)
          create(:csp_user, user: other_user, uuid: SecureRandom.uuid, csp:)
          post auth_endpoint
          follow_redirect!
        end
        it 'returns forbidden' do
          post '/update', params: params
          expect(response.status).to eq 403
        end

        it 'does not add emails' do
          expect do
            post '/update', params: params
          end.to change { UserEmail.count }.by(0)
        end
      end
    end
  end
end
