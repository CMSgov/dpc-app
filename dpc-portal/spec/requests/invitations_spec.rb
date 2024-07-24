# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Invitations', type: :request do
  RSpec.shared_examples 'an invitation endpoint' do |method, path_suffix|
    let(:org) { invitation.provider_organization }
    let(:bad_org) { create(:provider_organization) }
    context 'not logged in' do
      it 'should not show confirmation form if valid invitation' do
        send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
        expect(response).to be_ok
        expect(response.body).not_to include(confirm_organization_invitation_path(org, invitation))
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
            expect(response.body).to include('Request new invite')
          else
            expect(response.body).to_not include('Request new invite')
          end
        end
        it 'should not show renew button if accepted' do
          invitation.accept!
          send method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}"
          expect(response).to be_forbidden
          expect(response.body).to_not include('Request new invite')
        end
      end
      it 'should show warning page if accepted' do
        invitation.accept!
        send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
        expect(response).to be_forbidden
        expect(response.body).to include(I18n.t('verification.accepted_status'))
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
  end

  describe 'GET /accept' do
    shared_examples 'an accept endpoint' do
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
          it 'should show error page if email not match' do
            stub_user_info(overrides: { 'all_emails' => ['another@example.com'] })
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
            expect(response).to be_forbidden
          end
          it 'should show server error page if server error' do
            user_service_class = class_double(UserInfoService).as_stubbed_const
            allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'yikes')
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
            expect(response.status).to eq 503
            expect(response.body).to include(I18n.t('verification.server_error_status'))
          end
        end
      end
    end

    context :ao do
      it_behaves_like 'an invitation endpoint', :get, 'accept' do
        let(:invitation) { create(:invitation, :ao) }
      end
      it_behaves_like 'an accept endpoint' do
        let(:invitation) { create(:invitation, :ao) }
      end
      context 'logged in' do
        let(:invitation) { create(:invitation, :ao) }
        let(:org) { invitation.provider_organization }
        context :failure do
          it 'should show step 2' do
            user_service_class = class_double(UserInfoService).as_stubbed_const
            allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'yikes')
            log_in
            get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
            expect(response.status).to eq 503
            expect(response.body).to include('<span class="usa-step-indicator__current-step">2</span>')
          end
        end
      end
    end

    context :cd do
      it_behaves_like 'an invitation endpoint', :get, 'accept' do
        let(:invitation) { create(:invitation, :cd) }
      end
      it_behaves_like 'an accept endpoint' do
        let(:invitation) { create(:invitation, :cd) }
      end

      context 'logged in' do
        let(:verification_code) { 'ABC123' }
        let(:cd_invite) { create(:invitation, :cd, verification_code:) }
        let(:org) { cd_invite.provider_organization }
        before { log_in }
        context :success do
          before { stub_user_info }
          it 'should not show verification code' do
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
            expect(response.body).to_not include(cd_invite.verification_code)
          end
        end
        context :failure do
          it 'should render error page if family_name not match' do
            stub_user_info(overrides: { 'family_name' => 'Something Else' })
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
            expect(response).to be_forbidden
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
          end
          it 'should render error page if phone not match' do
            stub_user_info(overrides: { 'phone' => '9999999999' })
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
            expect(response).to be_forbidden
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
          end
          it 'should not show step navigation' do
            user_service_class = class_double(UserInfoService).as_stubbed_const
            allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'yikes')
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
            expect(response.status).to eq 503
            expect(response.body).to_not include('<span class="usa-step-indicator__current-step">')
          end
        end
      end
    end
  end

  describe 'POST /confirm' do
    shared_examples 'a confirm endpoint' do
      let(:org) { invitation.provider_organization }
      before { log_in }
      context :success do
        it 'should set session status to conditions verified' do
          stub_user_info
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm", params: success_params
          expect(response).to be_ok
          expect(request.session["invitation_status_#{invitation.id}"]).to eq 'conditions_verified'
        end
      end
      context :failure do
        it 'should not confirm if not passed identity verification' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm", params: success_params
          expect(response).to redirect_to(accept_organization_invitation_path(org, invitation))
          expect(request.session["invitation_status_#{invitation.id}"]).to be_nil
        end
      end
    end

    context :ao do
      it_behaves_like 'an invitation endpoint', :post, 'confirm' do
        let(:invitation) { create(:invitation, :ao) }
      end
      it_behaves_like 'a confirm endpoint' do
        let(:invitation) { create(:invitation, :ao) }
        let(:success_params) { {} }
      end
      context :success do
        let(:invitation) { create(:invitation, :ao) }
        let(:org) { invitation.provider_organization }
        before do
          stub_user_info
          log_in
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end
        it 'sets pac id' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(request.session[:user_pac_id]).to eq user_info['social_security_number']
        end
      end
      context :failure do
        let(:invitation) { create(:invitation, :ao) }
        let(:org) { invitation.provider_organization }
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
      end
    end

    context :cd do
      it_behaves_like 'an invitation endpoint', :post, 'confirm' do
        let(:invitation) { create(:invitation, :cd) }
      end
      it_behaves_like 'a confirm endpoint' do
        let(:verification_code) { 'ABC123' }
        let(:invitation) { create(:invitation, :cd, verification_code:) }
        let(:success_params) { { verification_code: } }
      end

      context :failure do
        let(:verification_code) { 'ABC123' }
        let(:cd_invite) { create(:invitation, :cd, verification_code:) }
        let(:org) { cd_invite.provider_organization }
        let(:fail_params) { { verification_code: 'badcode' } }
        context :accepted do
          before do
            log_in
            stub_user_info
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          end
          it 'should render form with error if OTP not match' do
            post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: fail_params
            expect(response).to be_bad_request
            expect(response.body).to include(confirm_organization_invitation_path(org, cd_invite))
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
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm", params: success_params
        end
        it 'should create link to organization' do
          klass = invitation.authorized_official? ? AoOrgLink : CdOrgLink
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register",
                 params: success_params
          end.to change { klass.count }.by(1)
        end

        it 'should update invitation' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          invitation.reload
          expect(invitation.invited_given_name).to be_blank
          expect(invitation.invited_family_name).to be_blank
          expect(invitation.invited_phone).to be_blank
          expect(invitation.invited_email).to be_blank
        end
        it 'should clear session variable' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          expect(request.session["invitation_status_#{invitation.id}"]).to be_nil
        end
        it 'should show success page' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          expect(response).to be_ok
          expect(response.body).to include('Registration completed')
        end

        it 'should create user if not exist' do
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          end.to change { User.count }.by 1
        end
        it 'should not create user if exists' do
          create(:user, provider: :openid_connect, uid: user_info['sub'])
          expect do
            post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          end.to change { User.count }.by 0
        end
        it 'should not override pac_id on existing user' do
          create(:user, provider: :openid_connect, uid: user_info['sub'], pac_id: :foo)
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info['sub'])
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(user.pac_id).to eq 'foo'
        end
      end

      context 'failure' do
        before do
          stub_user_info
          get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        end
        it 'should redirect if not confirmed' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          expect(response).to redirect_to(accept_organization_invitation_path(org, invitation))
        end
      end
    end

    context :cd do
      it_behaves_like 'an invitation endpoint', :post, 'register' do
        let(:invitation) { create(:invitation, :cd) }
      end
      it_behaves_like 'a register endpoint' do
        let(:verification_code) { 'ABC123' }
        let(:invitation) { create(:invitation, :cd, verification_code:) }
        let(:success_params) { { verification_code: } }
      end
    end
    context :ao do
      it_behaves_like 'an invitation endpoint', :post, 'register' do
        let(:invitation) { create(:invitation, :ao) }
      end
      it_behaves_like 'a register endpoint' do
        let(:invitation) { create(:invitation, :ao) }
        let(:success_params) { {} }
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
        it 'should set pac_id' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info['sub'])
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(user.pac_id).to eq user_info['social_security_number']
          expect(request.session[:user_pac_id]).to be_nil
        end
        it 'should set pac_id on existing user' do
          create(:user, provider: :openid_connect, uid: user_info['sub'])
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          user = User.find_by(uid: user_info['sub'])
          # We have the fake CPI API Gateway return the ssn as pac_id
          expect(user.pac_id).to eq user_info['social_security_number']
          expect(request.session[:user_pac_id]).to be_nil
        end
        it 'should sign in user' do
          post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
          get '/'
          expect(response).to be_ok
          links = assigns(:links)
          expect(links.size).to eq 1
          expect(links.first.provider_organization).to eq org
        end
      end
    end
  end

  describe 'POST /login' do
    let(:invitation) { create(:invitation, :cd) }
    it 'should redirect to login.gov' do
      org_id = invitation.provider_organization.id
      post "/organizations/#{org_id}/invitations/#{invitation.id}/login"
      redirect_params = Rack::Utils.parse_query(URI.parse(response.location).query)
      expect(redirect_params['acr_values']).to eq('http://idmanagement.gov/ns/assurance/ial/2')
      expect(redirect_params['redirect_uri'][...29]).to eq 'http://localhost:3100/portal/'
      expected_redirect = accept_organization_invitation_url(org_id, invitation)
      expect(request.session[:user_return_to]).to eq expected_redirect
    end

    it 'should show warning page with 404 if missing' do
      bad_org = create(:provider_organization)
      post "/organizations/#{bad_org.id}/invitations/#{invitation.id}/login"
      expect(response).to be_not_found
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

def user_info(overrides = {})
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
    'phone' => '+1111111111',
    'phone_verified' => true,
    'verified_at' => 1_704_834_157,
    'ial' => 'http://idmanagement.gov/ns/assurance/ial/2',
    'aal' => 'urn:gov:gsa:ac:classes:sp:PasswordProtectedTransport:duo'
  }.merge(overrides)
end

def stub_user_info(overrides: {})
  user_service_class = class_double(UserInfoService).as_stubbed_const
  user_service = double(UserInfoService)
  expect(user_service_class).to receive(:new).at_least(:once).and_return(user_service)

  expect(user_service).to receive(:user_info).at_least(:once).and_return(user_info(overrides))
end
