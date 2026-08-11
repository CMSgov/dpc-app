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
                                                     { actionContext: LoggingConstants::ActionContext::Authentication,
                                                       actionType: LoggingConstants::ActionType::UserLoggedIn,
                                                       csp: csp_name }])
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
        expect(response.body).to include('Add new email')
        expect(response.body).to include(root_path)
      end

      it 'logs about existing account' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User has existing account associated with different email',
                                                     { actionContext: LoggingConstants::ActionContext::Authentication,
                                                       actionType: LoggingConstants::ActionType::MergeUserAccountEmail,
                                                       csp: csp_name }])
        post auth_endpoint
        follow_redirect!
      end
    end

    context 'user exists with different CSP' do
      let(:email) { 'bob@example.com' }
      let(:orig_csp_name) { (CspUtils::CODES_TO_DISPLAY.keys - [provider]).sample.to_s }
      context 'SSN matches' do
        before do
          user = create(:user)
          orig_csp = Csp.find_by(name: orig_csp_name) || create(:csp, name: orig_csp_name)
          csp_user = create(:csp_user, user:, uuid:, csp: orig_csp)
          create(:user_email, csp_user:, email:, primary: true, active: true)

          stub_request(:get, CspUtils.user_info_url(provider))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: csp_auth_response.to_json, status: 200)
          stub_request(:get, CspUtils.user_info_url(orig_csp_name))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: csp_auth_response.to_json, status: 200)

          OmniAuth.config.add_mock(orig_csp_name, csp_auth_response)
          post "/auth/#{orig_csp_name}"
          follow_redirect!
        end
        it 'renders the link account component' do
          post auth_endpoint
          follow_redirect!
          expect(response).to be_ok
          expect(response.body).to include('Existing account found')
          expect(response.body).to include(EmailMask.masked(email))
          expect(response.body).to include(CspUtils.display_name(orig_csp_name))
          expect(response.body).to include('Link to existing account')
          expect(response.body).to include("/auth/#{orig_csp_name}")
        end

        it 'logs about existing account' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(['User has existing account associated with different CSP',
                                                       { actionContext: LoggingConstants::ActionContext::Authentication,
                                                         actionType: LoggingConstants::ActionType::MergeUserAccountCsp,
                                                         csp: orig_csp_name }])
          post auth_endpoint
          follow_redirect!
        end

        it 'signs in a user' do
          post auth_endpoint
          follow_redirect!
          post "/auth/#{orig_csp_name}"
          follow_redirect!
          expect(response.location).to eq organizations_url
          expect(response).to be_redirect
          follow_redirect!
          expect(response).to be_ok
        end

        it 'redirects to organizations path' do
          post auth_endpoint
          follow_redirect!
          expect(response).to be_ok
          post "/auth/#{orig_csp_name}"
          follow_redirect!
          expect(response).to redirect_to(organizations_path)
        end

        it 'creates a new CspUser for the current CSP' do
          expect do
            post auth_endpoint
            follow_redirect!
            post "/auth/#{orig_csp_name}"
            follow_redirect!
          end.to change { CspUser.count }.by(1)
        end
      end

      context 'SSN does not match' do
        let(:social_security_number) { '4-5-6' }
        let(:ssn_field) { provider == :clear ? :SSN : :social_security_number }
        let(:ssn_mismatch_response) do
          csp_auth_response.deep_dup.deep_merge(extra: { raw_info: { ssn_field => social_security_number } })
        end
        before do
          user = create(:user)
          orig_csp = Csp.find_by(name: orig_csp_name) || create(:csp, name: orig_csp_name)
          csp_user = create(:csp_user, user:, uuid:, csp: orig_csp)
          create(:user_email, csp_user:, email:, primary: true, active: true)

          stub_request(:get, CspUtils.user_info_url(orig_csp_name))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: csp_auth_response.to_json, status: 200)
          stub_request(:get, CspUtils.user_info_url(provider))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: ssn_mismatch_response.to_json, status: 200)

          OmniAuth.config.add_mock(orig_csp_name, csp_auth_response)
          post "/auth/#{orig_csp_name}"
          follow_redirect!
        end

        it 'does not sign in a user' do
          post auth_endpoint
          follow_redirect!
          expect do
            post "/auth/#{orig_csp_name}"
            follow_redirect!
          end.to raise_error(CspExistingAccount::SsnMismatchError, 'SSN mismatch')
          expect(response).not_to redirect_to(organizations_path)
        end

        it 'does not create a new CspUser' do
          post auth_endpoint
          follow_redirect!
          expect do
            begin
              post "/auth/#{orig_csp_name}"
              follow_redirect!
            rescue CspExistingAccount::SsnMismatchError
              nil
            end
          end.to change { CspUser.count }.by(0)
        end
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
        expect(response.location).to eq organizations_url
        expect(response).to be_redirect
        follow_redirect!
        expect(response).to be_redirect
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
           { actionContext: LoggingConstants::ActionContext::Authentication,
             actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }]
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
         { actionContext: LoggingConstants::ActionContext::Authentication,
           actionType: LoggingConstants::ActionType::InvalidCsp }]
      )
      post auth_endpoint
      follow_redirect!
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
