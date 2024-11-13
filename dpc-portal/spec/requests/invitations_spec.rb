# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Invitations', type: :request do
  RSpec.shared_examples 'an invitation endpoint' do |method, path_suffix|
    let(:org) { invitation.provider_organization }
    let(:bad_org) { create(:provider_organization) }
    let(:expected_success_status) { 200 }
    it 'should be ok or redirect' do
      send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
      expect(response.status).to eq(expected_success_status)
    end
    it 'should show warning page with 404 if missing' do
      send(method, "/organizations/#{org.id}/invitations/bad-id/#{path_suffix}")
      expect(response).to be_not_found
      expect(response.body).to include(I18n.t('verification.invalid_status'))
    end
    it 'should show warning page with 404 if org-invitation mismatch' do
      bad_org = create(:provider_organization)
      send(method, "/organizations/#{bad_org.id}/invitations/#{invitation.id}/#{path_suffix}")
      expect(response).to be_not_found
      expect(response.body).to include(I18n.t('verification.invalid_status'))
    end
    it 'should show warning page if cancelled' do
      invitation.update(status: :cancelled)
      send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
      expect(response).to be_forbidden
      expect(response.body).to include(I18n.t('verification.invalid_status'))
    end
    context 'invitation expired' do
      before { invitation.update_attribute(:created_at, 3.days.ago) }
      it 'should show warning page' do
        send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
        expect(response).to be_forbidden
      end
      it 'should show renew button only if ao' do
        send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
        expect(response).to be_forbidden
        if invitation.authorized_official?
          expect(response.body).to include('Request new link')
        else
          expect(response.body).to_not include('Request new link')
        end
      end
      it 'logs if invitation is expired' do
        if invitation.credential_delegate?
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['Credential Delegate Invitation expired',
             { actionContext: LoggingConstants::ActionContext::Registration,
               actionType: LoggingConstants::ActionType::CdInvitationExpired }]
          )
        elsif invitation.authorized_official?
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['Authorized Official Invitation expired',
             { actionContext: LoggingConstants::ActionContext::Registration,
               actionType: LoggingConstants::ActionType::AoInvitationExpired }]
          )
        end
        send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
      end
      it 'should not show renew button if accepted' do
        invitation.accept!
        send method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}"
        expect(response).to be_forbidden
        expect(response.body).to_not include('Request new link')
      end
    end
    it 'should show warning page if accepted' do
      invitation.accept!
      send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
      expect(response).to be_forbidden
      if invitation.authorized_official?
        expect(response.body).to include(I18n.t('verification.ao_accepted_status'))
      elsif invitation.credential_delegate?
        expect(response.body).to include(I18n.t('verification.cd_accepted_status'))
      end
    end
  end

  describe 'GET /' do
    context :ao do
      let(:ao_invite) { create(:invitation, :ao) }
      let(:org) { ao_invite.provider_organization }

      it_behaves_like 'an invitation endpoint', :get, '' do
        let(:invitation) { create(:invitation, :ao) }
      end

      it 'should show button to accept' do
        get "/organizations/#{org.id}/invitations/#{ao_invite.id}"
        expect(response).to be_ok
        expect(response.body).to include(accept_organization_invitation_path(org, ao_invite))
      end
    end
    context :cd do
      let(:cd_invite) { create(:invitation, :cd) }
      let(:org) { cd_invite.provider_organization }

      it_behaves_like 'an invitation endpoint', :get, '' do
        let(:invitation) { create(:invitation, :cd) }
      end

      it 'should show button to confirm identity' do
        get "/organizations/#{org.id}/invitations/#{cd_invite.id}"
        expect(response).to be_ok
        expect(response.body).to include(confirm_cd_organization_invitation_path(org, cd_invite))
      end
    end
  end

  describe 'POST /login' do
    RSpec.shared_examples 'a login endpoint' do
      it 'should redirect to login.gov' do
        org_id = invitation.provider_organization.id
        post "/organizations/#{org_id}/invitations/#{invitation.id}/login"
        redirect_params = Rack::Utils.parse_query(URI.parse(response.location).query)
        expect(redirect_params['acr_values']).to eq('http://idmanagement.gov/ns/assurance/ial/2')
        expect(redirect_params['redirect_uri'][...29]).to eq 'http://localhost:3100/portal/'
        expect(request.session[:user_return_to]).to eq expected_redirect
      end

      it 'should log that user has begun login' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User began login flow',
                                                     { actionContext: LoggingConstants::ActionContext::Registration,
                                                       actionType: LoggingConstants::ActionType::BeginLogin }])
        org_id = invitation.provider_organization.id
        post "/organizations/#{org_id}/invitations/#{invitation.id}/login"
      end

      it 'should show error page if fail to proof' do
        org_id = invitation.provider_organization.id
        post "/organizations/#{org_id}/invitations/#{invitation.id}/login"
        get '/users/auth/failure'
        expect(response).to be_forbidden
        expect(response.body).to include(I18n.t('verification.fail_to_proof_text'))
      end
    end

    context :cd do
      it_behaves_like 'an invitation endpoint', :post, 'login' do
        let(:invitation) { create(:invitation, :cd) }
        let(:expected_success_status) { 302 }
      end
      it_behaves_like 'a login endpoint', :post, 'register' do
        let(:invitation) { create(:invitation, :cd) }
        let(:expected_redirect) do
          confirm_cd_organization_invitation_url(invitation.provider_organization.id, invitation)
        end
      end
      context 'fail to proof' do
        let(:invitation) { create(:invitation, :cd) }
        it 'should not show step navigation' do
          org_id = invitation.provider_organization.id
          post "/organizations/#{org_id}/invitations/#{invitation.id}/login"
          get '/users/auth/failure'
          expect(response).to be_forbidden
          expect(response.body).to_not include('<span class="usa-step-indicator__current-step">')
        end
      end
    end
    context :ao do
      it_behaves_like 'an invitation endpoint', :post, 'login' do
        let(:invitation) { create(:invitation, :ao) }
        let(:expected_success_status) { 302 }
      end
      it_behaves_like 'a login endpoint', :post, 'register' do
        let(:invitation) { create(:invitation, :ao) }
        let(:expected_redirect) { accept_organization_invitation_url(invitation.provider_organization.id, invitation) }
      end
      context 'fail to proof' do
        let(:invitation) { create(:invitation, :ao) }
        it 'should show step 2' do
          org_id = invitation.provider_organization.id
          post "/organizations/#{org_id}/invitations/#{invitation.id}/login"
          get '/users/auth/failure'
          expect(response).to be_forbidden
          expect(response.body).to include('<span class="usa-step-indicator__current-step">2</span>')
        end
      end
    end
  end

  describe 'GET /accept' do
    it_behaves_like 'an invitation endpoint', :get, 'accept' do
      let(:invitation) { create(:invitation, :ao) }
    end
    let(:invitation) { create(:invitation, :ao) }
    let(:org) { invitation.provider_organization }

    context 'not logged in' do
      it 'should show login if valid invitation' do
        get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        expect(response).to be_ok
        expect(response.body).to include(login_organization_invitation_path(org, invitation))
      end
    end

    context 'logged in' do
      before { log_in }
      it 'should show login if token expired' do
        user_service_class = class_double(UserInfoService).as_stubbed_const
        allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'unauthorized')
        get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        expect(response).to be_ok
        expect(response.body).to include(login_organization_invitation_path(org, invitation))
      end
      context :success do
        before { stub_user_info }
        it 'should show confirm form if valid invitation' do
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          expect(response).to be_ok
          expect(response.body).to include(confirm_organization_invitation_path(org, invitation))
          expect(request.session["invitation_status_#{invitation.id}"]).to eq 'identity_verified'
        end
      end
      context :failure do
        it 'should fail if cd invitation' do
          cd_invite = create(:invitation, :cd)
          org = cd_invite.provider_organization
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to redirect_to(organization_invitation_path(org, cd_invite))
        end
        it 'should show error page if email not match' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(
            ['AO PII Check Fail',
             { actionContext: LoggingConstants::ActionContext::Registration,
               actionType: LoggingConstants::ActionType::FailAoPiiCheck,
               invitation: invitation.id }]
          )
          stub_user_info(overrides: { 'email' => 'another@example.com' })
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          expect(assigns(:given_name)).to be_nil
          expect(response).to be_forbidden
          expect(response.body).to include(CGI.escapeHTML(I18n.t('verification.email_mismatch_status')))
        end
        context :server_error do
          it 'should show server error page' do
            user_service_class = class_double(UserInfoService).as_stubbed_const
            allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'server_error')
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
            expect(response.status).to eq 503
            expect(response.body).to include(I18n.t('verification.server_error_status'))
          end
          it 'should show step 2' do
            user_service_class = class_double(UserInfoService).as_stubbed_const
            allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'server_error')
            log_in
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
            expect(response.status).to eq 503
            expect(response.body).to include('<span class="usa-step-indicator__current-step">2</span>')
          end
        end
      end
    end
  end

  describe 'POST /confirm' do
    it_behaves_like 'an invitation endpoint', :post, 'confirm' do
      let(:invitation) { create(:invitation, :ao) }
    end
    context :success do
      let(:invitation) { create(:invitation, :ao) }
      let(:org) { invitation.provider_organization }
      before do
        stub_user_info
        log_in
        get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
      end
      it 'should set session status to conditions verified' do
        post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
        expect(response).to be_ok
        expect(request.session["invitation_status_#{invitation.id}"]).to eq 'verification_complete'
      end
      it 'sets pac id' do
        post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
        # We have the fake CPI API Gateway return the ssn as pac_id
        expect(request.session[:user_pac_id]).to eq user_info_template['social_security_number']
      end
      context :ao_has_waiver do
        let(:invitation) { create(:invitation, :ao) }
        let(:org) { invitation.provider_organization }
        before do
          stub_user_info(overrides: { 'social_security_number' => '900777777' })
          log_in
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end
        it 'logs a waiver' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info)
            .with(['Authorized official has a waiver',
                   { actionContext: LoggingConstants::ActionContext::Registration,
                     actionType: LoggingConstants::ActionType::AoHasWaiver,
                     invitation: invitation.id }])
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
        end
      end
      context :org_has_waiver do
        let(:org) { create(:provider_organization, npi: '3098168743', verification_status: :approved) }
        let(:invitation) { create(:invitation, :ao, provider_organization: org) }
        before { log_in }
        it 'should log a waiver' do
          stub_user_info
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info)
            .with(['Organization has a waiver',
                   { actionContext: LoggingConstants::ActionContext::Registration,
                     actionType: LoggingConstants::ActionType::OrgHasWaiver,
                     invitation: invitation.id }])
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
        end
      end
    end
    context :failure do
      let(:invitation) { create(:invitation, :ao) }
      let(:org) { invitation.provider_organization }
      context :process do
        before { log_in }
        it 'should fail if cd invitation' do
          cd_invite = create(:invitation, :cd)
          org = cd_invite.provider_organization
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm"
          expect(response).to redirect_to(organization_invitation_path(org, cd_invite))
        end

        it 'should not confirm if not passed identity verification' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response).to redirect_to(accept_organization_invitation_path(org, invitation))
          expect(request.session["invitation_status_#{invitation.id}"]).to be_nil
        end
      end

      context 'cpi api gateway check' do
        before do
          stub_user_info(overrides: { 'social_security_number' => '000000000' })
          log_in
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end
        it 'renders not-ao error' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response).to be_forbidden
          expect(response.body).to include(I18n.t('verification.user_not_authorized_official_status'))
        end
        it 'renders step 3' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response).to be_forbidden
          expect(response.body).to include('<span class="usa-step-indicator__current-step">3</span>')
        end
        it 'logs failure' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(['AO Check Fail',
                                                       { actionContext: LoggingConstants::ActionContext::Registration,
                                                         actionType: LoggingConstants::ActionType::FailCpiApiGwCheck,
                                                         verificationReason: 'user_not_authorized_official',
                                                         invitation: invitation.id }])
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
        end
      end

      context 'login.gov server error' do
        before do
          user_service_class = class_double(UserInfoService).as_stubbed_const
          user_service = double(UserInfoService)
          expect(user_service_class).to receive(:new).at_least(:once).and_return(user_service)

          expect(user_service).to receive(:user_info).and_invoke(proc { user_info_template },
                                                                 proc { raise UserInfoServiceError, 'server_error' })

          log_in
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end

        it 'should show server error' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response.status).to eq 503
          expect(response.body).to include(I18n.t('verification.server_error_status'))
        end

        it 'should show step 3' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response.status).to eq 503
          expect(response.body).to include('<span class="usa-step-indicator__current-step">3</span>')
        end
      end

      context 'login.gov missing ssn' do
        before do
          stub_user_info(overrides: { 'social_security_number' => '' })
          log_in
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end

        it 'should show missing info error' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response.status).to eq 503
          expect(response.body).to include(I18n.t('verification.missing_info_text'))
        end

        it 'should show step 3' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response.status).to eq 503
          expect(response.body).to include('<span class="usa-step-indicator__current-step">3</span>')
        end
      end

      context 'cpi api gateway server error' do
        before do
          cpi_api_gateway_client_class = class_double(CpiApiGatewayClient).as_stubbed_const
          cpi_api_gateway_client = double(CpiApiGatewayClient)
          expect(cpi_api_gateway_client_class).to receive(:new).at_least(:once).and_return(cpi_api_gateway_client)
          expect(cpi_api_gateway_client).to receive(:fetch_profile).and_raise(
            OAuth2::Error, Faraday::Response.new(status: 500)
          )
          stub_user_info
          log_in
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end

        it 'should show server error' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response.status).to eq 503
          expect(response.body).to include(I18n.t('verification.server_error_status'))
        end

        it 'should show step 3' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          expect(response.status).to eq 503
          expect(response.body).to include('<span class="usa-step-indicator__current-step">3</span>')
        end
      end
    end
  end

  describe 'GET /confirm_cd' do
    it_behaves_like 'an invitation endpoint', :get, 'confirm_cd' do
      let(:invitation) { create(:invitation, :cd) }
    end
    context 'logged in' do
      let(:cd_invite) { create(:invitation, :cd) }
      let(:org) { cd_invite.provider_organization }
      before { log_in }
      context 'passed identity confirmation' do
        context :success do
          it 'should show register' do
            stub_user_info
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
            expect(response.body).to include(register_organization_invitation_path(org, cd_invite))
          end
          it 'should set verification complete' do
            stub_user_info
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
            expect(request.session["invitation_status_#{cd_invite.id}"]).to eq 'verification_complete'
          end
          it 'should log approved access for CD' do
            stub_user_info
            allow(Rails.logger).to receive(:info)
            approved_access_log_message = [
              'Approved access authorization occurred for the Credential Delegate',
              { actionContext: LoggingConstants::ActionContext::Registration,
                actionType: LoggingConstants::ActionType::CdConfirmed }
            ]
            expect(Rails.logger).to receive(:info).with(approved_access_log_message)
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
          end
          it 'should ignore given name' do
            stub_user_info(overrides: { 'given_name' => 'Something Else' })
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
            expect(response.body).to include(register_organization_invitation_path(org, cd_invite))
            expect(request.session["invitation_status_#{cd_invite.id}"]).to eq 'verification_complete'
          end
        end
        context :failure do
          it 'should render error page if email not match' do
            allow(Rails.logger).to receive(:info)
            expect(Rails.logger).to receive(:info).with(
              ['CD PII Check Fail',
               { actionContext: LoggingConstants::ActionContext::Registration,
                 actionType: LoggingConstants::ActionType::FailCdPiiCheck,
                 invitation: cd_invite.id }]
            )
            stub_user_info(overrides: { 'email' => 'another@example.com' })
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
            expect(assigns(:given_name)).to be_nil
            expect(response).to be_forbidden
            expect(response.body).to include(CGI.escapeHTML(I18n.t('verification.email_mismatch_status')))
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
          end
          it 'should render error page if family_name not match' do
            allow(Rails.logger).to receive(:info)
            expect(Rails.logger).to receive(:info).with(
              ['CD PII Check Fail',
               { actionContext: LoggingConstants::ActionContext::Registration,
                 actionType: LoggingConstants::ActionType::FailCdPiiCheck,
                 invitation: cd_invite.id }]
            )
            stub_user_info(overrides: { 'family_name' => 'Something Else' })
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
            expect(response).to be_forbidden
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
          end
          it 'should not show step navigation' do
            user_service_class = class_double(UserInfoService).as_stubbed_const
            allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'server_error')
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm_cd"
            expect(response.status).to eq 503
            expect(response.body).to_not include('<span class="usa-step-indicator__current-step">')
          end
          it 'should fail if ao invitation' do
            ao_invite = create(:invitation, :ao)
            org = ao_invite.provider_organization
            get "/organizations/#{org.id}/invitations/#{ao_invite.id}/confirm_cd"
            expect(response).to redirect_to(organization_invitation_path(org, ao_invite))
          end
        end
      end
    end
  end

  describe 'POST /register' do
    shared_examples 'a register endpoint' do
      let(:org) { invitation.provider_organization }
      before { log_in }
      context :success do
        before do
          stub_user_info
          if invitation.authorized_official?
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
            post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          else
            get "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
          end
        end
        it 'should show success page' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          expect(response).to be_ok
          expect(response.body).to include('Go to DPC Portal')
        end
        it 'should update invitation' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          invitation.reload
          expect(invitation.invited_given_name).to be_blank
          expect(invitation.invited_family_name).to be_blank
          expect(invitation.invited_email).to be_blank
        end
        it 'should clear session variable' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          expect(request.session["invitation_status_#{invitation.id}"]).to be_nil
        end
        it 'should create link to organization' do
          klass = invitation.authorized_official? ? AoOrgLink : CdOrgLink
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          end.to change { klass.count }.by(1)
        end
        it 'should log that link was created' do
          allow(Rails.logger).to receive(:info)
          if invitation.authorized_official?
            expect(Rails.logger).to receive(:info).with(['Authorized Official linked to organization',
                                                         { actionContext: LoggingConstants::ActionContext::Registration,
                                                           actionType: LoggingConstants::ActionType::AoLinkedToOrg }])
          else
            expect(Rails.logger).to receive(:info).with(['Credential Delegate linked to organization',
                                                         { actionContext: LoggingConstants::ActionContext::Registration,
                                                           actionType: LoggingConstants::ActionType::CdLinkedToOrg }])
          end
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
        end

        it 'should log user logged in' do
          allow(Rails.logger).to receive(:info)
          expect(Rails.logger).to receive(:info).with(['User logged in',
                                                       { actionContext: LoggingConstants::ActionContext::Registration,
                                                         actionType: LoggingConstants::ActionType::UserLoggedIn }])
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
        end

        it 'should create user if not exist' do
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          end.to change { User.count }.by 1
          user = User.last
          expect(user.given_name).to eq user_info_template['given_name']
          expect(user.family_name).to eq user_info_template['family_name']
        end

        it 'should log when user is created' do
          allow(Rails.logger).to receive(:info)
          if invitation.authorized_official?
            expect(Rails.logger).to receive(:info).with(['Authorized Official user created,',
                                                         { actionContext: LoggingConstants::ActionContext::Registration,
                                                           actionType: LoggingConstants::ActionType::AoCreated }])
          else
            expect(Rails.logger).to receive(:info).with(['Credential Delegate user created,',
                                                         { actionContext: LoggingConstants::ActionContext::Registration,
                                                           actionType: LoggingConstants::ActionType::CdCreated }])
          end
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
        end
        it 'should not create user if exists' do
          create(:user, provider: :openid_connect, uid: user_info_template['sub'])
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          end.to change { User.count }.by 0
        end
        it 'should update name of user if changed' do
          user = create(:user, provider: :openid_connect, uid: user_info_template['sub'], given_name: :foo,
                               family_name: :bar)
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          end.to change { User.count }.by 0
          user.reload
          expect(user.given_name).to eq user_info_template['given_name']
          expect(user.family_name).to eq user_info_template['family_name']
        end
        it 'should not override pac_id on existing user' do
          create(:user, provider: :openid_connect, uid: user_info_template['sub'], pac_id: :foo)
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info_template['sub'])
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(user.pac_id).to eq 'foo'
        end
        it 'should be able to immediate view organizations' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          get '/'
          expect(response).to be_ok
          links = assigns(:links)
          expect(links.size).to eq 1
          expect(links.first.provider_organization).to eq org
        end
      end

      context 'failure' do
        before do
          if invitation.authorized_official?
            stub_user_info
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          end
        end
        it 'should redirect if not confirmed' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          expect(response).to redirect_to(organization_invitation_path(org, invitation))
        end
      end
    end

    context :cd do
      it_behaves_like 'an invitation endpoint', :post, 'register' do
        let(:invitation) { create(:invitation, :cd) }
      end
      it_behaves_like 'a register endpoint' do
        let(:invitation) { create(:invitation, :cd) }
      end
      context :success do
        let(:invitation) { create(:invitation, :cd) }
        let(:org) { invitation.provider_organization }
        before do
          log_in
          stub_user_info
          get "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
        end
        it 'should not save verification_status on user and org' do
          create(:user, provider: :openid_connect, uid: user_info_template['sub'], pac_id: :foo)
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info_template['sub'])
          expect(user.verification_status).to be_nil
          expect(org.reload.verification_status).to be_nil
        end
      end
    end
    context :ao do
      it_behaves_like 'an invitation endpoint', :post, 'register' do
        let(:invitation) { create(:invitation, :ao) }
      end
      it_behaves_like 'a register endpoint' do
        let(:invitation) { create(:invitation, :ao) }
      end
      context :success do
        let(:invitation) { create(:invitation, :ao) }
        let(:org) { invitation.provider_organization }
        before do
          log_in
          stub_user_info
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
        end
        it 'should set pac_id on new user' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info_template['sub'])
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(user.pac_id).to eq user_info_template['social_security_number']
          expect(request.session[:user_pac_id]).to be_nil
        end
        it 'should set pac_id on existing user' do
          create(:user, provider: :openid_connect, uid: user_info_template['sub'])
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info_template['sub'])
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(user.pac_id).to eq user_info_template['social_security_number']
          expect(request.session[:user_pac_id]).to be_nil
        end
        it 'should save verification_status on user and org' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info_template['sub'])
          expect(user.verification_status).to eq('approved')
          expect(org.reload.verification_status).to eq('approved')
        end
      end
    end
  end

  describe 'POST /renew' do
    let(:fail_message) { 'Unable to create new invitation' }
    context :ao do
      let(:invitation) { create(:invitation, :ao) }
      let(:org_id) { invitation.provider_organization.id }
      let(:success_message) { 'You should receive your new invitation shortly' }
      it 'should create another invitation for the user if expired' do
        invitation.update(created_at: 49.hours.ago)
        expect do
          post "/organizations/#{org_id}/invitations/#{invitation.id}/renew"
        end.to change { Invitation.count }.by(1)
        expect(flash[:notice]).to eq success_message
        expect(response).to redirect_to(accept_organization_invitation_path(org_id, invitation))
      end
      it 'should not create another invitation for the user if accepted' do
        invitation.accept!
        expect do
          post "/organizations/#{org_id}/invitations/#{invitation.id}/renew"
        end.to change { Invitation.count }.by(0)
        expect(flash[:alert]).to eq fail_message
        expect(response).to redirect_to(accept_organization_invitation_path(org_id, invitation))
      end

      it 'should not create another invitation for the user if cancelled' do
        invitation.update(status: :cancelled)
        expect do
          post "/organizations/#{org_id}/invitations/#{invitation.id}/renew"
        end.to change { Invitation.count }.by(0)
        expect(flash[:alert]).to eq fail_message
        expect(response).to redirect_to(accept_organization_invitation_path(org_id, invitation))
      end
    end

    context :cd do
      let!(:invitation) { create(:invitation, :cd, created_at: 49.hours.ago) }
      let(:org_id) { invitation.provider_organization.id }
      it 'should not create another invitation for the user' do
        expect do
          post "/organizations/#{org_id}/invitations/#{invitation.id}/renew"
        end.to change { Invitation.count }.by(0)
        expect(flash[:alert]).to eq fail_message
        expect(response).to redirect_to(accept_organization_invitation_path(org_id, invitation))
      end
    end
  end

  describe 'GET /set_idp_token' do
    let(:invitation) { create(:invitation, :ao) }
    let(:org_id) { invitation.provider_organization.id }

    it_behaves_like 'an invitation endpoint', :get, 'set_idp_token' do
      let(:invitation) { create(:invitation, :ao) }
    end

    it 'should succeed in Rails.test' do
      get "/organizations/#{org_id}/invitations/#{invitation.id}/set_idp_token"
      expect(response).to be_ok
      expect(response.body).to be_blank
    end
    it 'should fail outside Rails.test' do
      allow(Rails.env).to receive(:test?).and_return false
      get "/organizations/#{org_id}/invitations/#{invitation.id}/set_idp_token"
      expect(response).to be_forbidden
    end
  end
end

def log_in
  OmniAuth.config.test_mode = true
  OmniAuth.config.add_mock(:openid_connect,
                           { uid: '12345',
                             credentials: { expires_in: 899,
                                            token: 'bearer-token' },
                             info: { email: 'bob@example.com' },
                             extra: { raw_info: { given_name: 'Bob',
                                                  family_name: 'Hoskins',
                                                  ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })
  post '/users/auth/openid_connect'
  follow_redirect!
end

def user_info_template(overrides = {})
  {
    'sub' => '097d06f7-e9ad-4327-8db3-0ba193b7a2c2',
    'iss' => 'https://idp.int.identitysandbox.gov/',
    'email' => 'bob@testy.com',
    'email_verified' => true,
    'all_emails' => [
      'bob@testy.com',
      'david@example.com',
      'david2@example.com'
    ],
    'given_name' => 'Bob',
    'family_name' => 'Hodges',
    'birthdate' => '1938-10-06',
    'social_security_number' => '900888888',
    'verified_at' => 1_704_834_157,
    'ial' => 'http://idmanagement.gov/ns/assurance/ial/2',
    'aal' => 'urn:gov:gsa:ac:classes:sp:PasswordProtectedTransport:duo'
  }.merge(overrides)
end

def stub_user_info(overrides: {})
  user_service_class = class_double(UserInfoService).as_stubbed_const
  user_service = double(UserInfoService)
  expect(user_service_class).to receive(:new).at_least(:once).and_return(user_service)

  expect(user_service).to receive(:user_info).at_least(:once).and_return(user_info_template(overrides))
end
