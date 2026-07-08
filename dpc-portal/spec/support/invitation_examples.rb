# frozen_string_literal: true

RSpec.shared_examples 'an invitation endpoint' do |method, path_suffix, role|
  let(:org) { invitation.provider_organization }
  let(:bad_org) { create(:provider_organization) }
  let(:expected_success_status) { 200 }
  let(:request_params) { {} }
  it 'should be ok or redirect' do
    stub_user_info
    # Most calls will be empty params, but for /login, param required to specify which IDP to use
    send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}", params: request_params)
    expect(response.status).to eq(expected_success_status)
  end
  it 'should show warning page with 404 if missing' do
    send(method, "/organizations/#{org.id}/invitations/bad-id/#{path_suffix}")
    expect(response).to be_not_found
    # Without an invitation, we don't know if it's for an AO or CD, so we default to AO in the error message.
    expect(response.body).to include(I18n.t('verification.ao_invalid_status'))
  end
  it 'should show warning page with 404 if org-invitation mismatch' do
    bad_org = create(:provider_organization)
    send(method, "/organizations/#{bad_org.id}/invitations/#{invitation.id}/#{path_suffix}")
    expect(response).to be_not_found
    expect(response.body).to include(I18n.t("verification.#{role}_invalid_status"))
  end
  it 'should show warning page if cancelled' do
    invitation.update(status: :cancelled)
    send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
    expect(response).to be_forbidden
    expect(response.body).to include(I18n.t("verification.#{role}_invalid_status"))
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
             actionType: LoggingConstants::ActionType::CdInvitationExpired,
             invitation: invitation.id }]
        )
      elsif invitation.authorized_official?
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(
          ['Authorized Official Invitation expired',
           { actionContext: LoggingConstants::ActionContext::Registration,
             actionType: LoggingConstants::ActionType::AoInvitationExpired,
             invitation: invitation.id }]
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
    if invitation.authorized_official?
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(
        ['Authorized Official Invitation already accepted',
         { actionContext: LoggingConstants::ActionContext::Registration,
           actionType: LoggingConstants::ActionType::AoAlreadyRegistered,
           invitation: invitation.id }]
      )
    elsif invitation.credential_delegate?
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(
        ['Credential Delegate Invitation already accepted',
         { actionContext: LoggingConstants::ActionContext::Registration,
           actionType: LoggingConstants::ActionType::CdAlreadyRegistered,
           invitation: invitation.id }]
      )
    end

    send(method, "/organizations/#{org.id}/invitations/#{invitation.id}/#{path_suffix}")
    expect(response).to be_forbidden
    if invitation.authorized_official?
      expect(response.body).to include(I18n.t('verification.ao_accepted_status'))
    elsif invitation.credential_delegate?
      expect(response.body).to include(I18n.t('verification.cd_accepted_status'))
    end
  end
end

RSpec.shared_examples 'a login endpoint' do
  let(:org_id) { invitation.provider_organization.id }
  let(:provider_params) { { provider: provider } }
  before { get "/organizations/#{org_id}/invitations/#{invitation.id}/set_idp_token", params: provider_params }
  it 'should redirect to provider callback' do
    post "/organizations/#{org_id}/invitations/#{invitation.id}/login", params: provider_params
    redirect_params = Rack::Utils.parse_query(URI.parse(response.location).query)
    expect(redirect_params['redirect_uri']).to eq("http://localhost:3100/auth/#{provider}/callback")
    expect(request.session[:user_return_to]).to eq expected_redirect
  end

  it 'should log that user has begun login' do
    allow(Rails.logger).to receive(:info)
    expect(Rails.logger).to receive(:info).with(['User began login flow',
                                                 { actionContext: LoggingConstants::ActionContext::Registration,
                                                   actionType: LoggingConstants::ActionType::BeginLogin,
                                                   csp: provider.to_s,
                                                   invitation: invitation.id }])
    org_id = invitation.provider_organization.id
    post "/organizations/#{org_id}/invitations/#{invitation.id}/login", params: provider_params
  end

  it 'should show error page if fail to proof' do
    org_id = invitation.provider_organization.id
    post "/organizations/#{org_id}/invitations/#{invitation.id}/login", params: provider_params
    get '/users/auth/failure'
    expect(response).to be_forbidden
    expect(response.body).to include(I18n.t('verification.fail_to_proof_text'))
  end
end

RSpec.shared_examples 'a register endpoint' do
  let(:org) { invitation.provider_organization }
  before { log_in(provider:) }
  context :success do
    before do
      stub_user_info
      get "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token", params: provider_params
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
                                                       actionType: LoggingConstants::ActionType::AoLinkedToOrg,
                                                       csp: provider.to_s,
                                                       invitation: invitation.id }])
      else
        expect(Rails.logger).to receive(:info).with(['Credential Delegate linked to organization',
                                                     { actionContext: LoggingConstants::ActionContext::Registration,
                                                       actionType: LoggingConstants::ActionType::CdLinkedToOrg,
                                                       csp: provider.to_s,
                                                       invitation: invitation.id }])
      end
      post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
    end

    it 'should log user logged in' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['User logged in',
                                                   { actionContext: LoggingConstants::ActionContext::Registration,
                                                     actionType: LoggingConstants::ActionType::UserLoggedIn,
                                                     csp: provider.to_s,
                                                     invitation: invitation.id }])
      post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
    end

    it 'should create user if not exist' do
      expect do
        post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
      end.to change { User.count }.by 1

      user = User.find_by!(
        given_name: user_info_template['given_name'],
        family_name: user_info_template['family_name'],
        email: user_info_template['email']
      )
      expect(user.uid).to eq user_info_template['sub']

      csp_user = user.csp_user_for(provider.to_s)
      expect(csp_user).to be_present

      csp_emails = csp_user.user_emails.map(&:email)
      expect(csp_emails).not_to be_empty
      expect(csp_emails).to include(*user_info_template['all_emails'])
    end

    it 'should log when user is created' do
      allow(Rails.logger).to receive(:info)
      if invitation.authorized_official?
        expect(Rails.logger).to receive(:info).with(['Authorized Official user created,',
                                                     { actionContext: LoggingConstants::ActionContext::Registration,
                                                       actionType: LoggingConstants::ActionType::AoCreated,
                                                       csp: provider.to_s,
                                                       invitation: invitation.id }])
      else
        expect(Rails.logger).to receive(:info).with(['Credential Delegate user created,',
                                                     { actionContext: LoggingConstants::ActionContext::Registration,
                                                       actionType: LoggingConstants::ActionType::CdCreated,
                                                       csp: provider.to_s,
                                                       invitation: invitation.id }])
      end
      post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
    end
    it 'should not create user if exists' do
      create(:user, pac_id: user_info_template['social_security_number'], email: 'bob@testy.com', provider:)
      expect do
        post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
      end.to change { User.count }.by 0
    end
    it 'should update name of user if changed' do
      user = create(:user, pac_id: user_info_template['social_security_number'],
                           email: 'bob@testy.com', given_name: :foo, family_name: :bar, provider:)
      expect do
        post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
      end.to change { User.count }.by 0
      user.reload
      expect(user.given_name).to eq user_info_template['given_name']
      expect(user.family_name).to eq user_info_template['family_name']
    end
    it 'should not override pac_id on existing user' do
      create(:user, email: user_info_template['email'], pac_id: :foo, provider:)
      post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
      expect(response).to be_ok
      user = User.find_by(email: user_info_template['email'])
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
      get "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token", params: provider_params
      if invitation.authorized_official?
        stub_user_info
        get "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
      end
    end
    it 'should redirect if not confirmed' do
      post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
      expect(response).to redirect_to(organization_invitation_path(org, invitation))
    end
    it 'should show login if token expired' do
      if invitation.authorized_official?
        post "/organizations/#{org.id}/invitations/#{invitation.id}/confirm"
      else
        stub_user_info
        get "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
      end
      user_service_class = class_double(UserInfoService).as_stubbed_const
      allow(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'unauthorized')
      post "/organizations/#{org.id}/invitations/#{invitation.id}/register"
      expect(response).to be_ok
      expect(response.body).to include(login_organization_invitation_path(org, invitation))
    end
  end
end
