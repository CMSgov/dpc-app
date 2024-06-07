# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Invitations', type: :request do
  describe 'GET /accept' do
    context :ao do
      context 'not logged in' do
        let!(:ao_invite) { create(:invitation, :ao, created_at: 3.days.ago) }
        let(:org) { ao_invite.provider_organization }
        it 'should show warning page' do
          get "/organizations/#{org.id}/invitations/#{ao_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show renew button' do
          get "/organizations/#{org.id}/invitations/#{ao_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('Request new invite')
        end
      end
    end

    context :cd do
      let(:invited_by) { create(:invited_by) }
      let(:verification_code) { 'ABC123' }
      let!(:cd_invite) { create(:invitation, :cd, verification_code:) }
      let(:user) do
        create(:user, given_name: cd_invite.invited_given_name,
                      family_name: cd_invite.invited_family_name,
                      email: cd_invite.invited_email)
      end
      let(:org) { cd_invite.provider_organization }

      context 'not logged in' do
        it 'should show login if valid invitation' do
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_ok
          expect(response.body).to include(login_organization_invitation_path(org, cd_invite))
        end
        it 'should not show form if valid invitation' do
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_ok
          expect(response.body).not_to include(confirm_organization_invitation_path(org, cd_invite))
        end
        it 'should show warning page with 404 if missing' do
          get "/organizations/#{org.id}/invitations/bad-id/accept"
          expect(response).to be_not_found
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page with 404 if org-invitation mismatch' do
          bad_org = create(:provider_organization)
          get "/organizations/#{bad_org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_not_found
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page if cancelled' do
          cd_invite.update(status: :cancelled)
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        context 'invitation expired' do
          before { cd_invite.update_attribute(:created_at, 3.days.ago) }
          it 'should show warning page' do
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
            expect(response).to be_forbidden
            expect(response.body).to include('usa-alert--warning')
          end
          it 'should not show renew button' do
            get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
            expect(response).to be_forbidden
            expect(response.body).to_not include('Request new invite')
          end
        end
        it 'should show warning page if accepted' do
          cd_invite.accept!
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
      end
      context 'logged in' do
        before do
          sign_in user
        end
        it 'should show form if valid invitation' do
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_ok
          expect(response.body).to include(confirm_organization_invitation_path(org, cd_invite))
        end
        it 'should not show verification code' do
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response.body).to_not include(cd_invite.verification_code)
        end
        it 'should show error page if email not match' do
          user.update_attribute(:email, 'another@example.com')
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--error')
        end
      end
    end
  end

  describe 'POST /confirm' do
    context :cd do
      let(:invited_by) { create(:invited_by) }
      let(:verification_code) { 'ABC123' }
      let!(:cd_invite) { create(:invitation, :cd, verification_code:) }
      let(:user) do
        create(:user, given_name: cd_invite.invited_given_name,
                      family_name: cd_invite.invited_family_name,
                      email: cd_invite.invited_email)
      end
      let(:org) { cd_invite.provider_organization }

      let(:success_params) { { verification_code: } }
      let(:fail_params) { { verification_code: 'badcode' } }

      before { sign_in user }

      context 'success' do
        before { stub_user_info }

        it 'should create CdOrgLink' do
          expect do
            post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm",
                 params: success_params
          end.to change { CdOrgLink.count }.by(1)
        end
        it 'should update invitation' do
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          cd_invite.reload
          expect(cd_invite.invited_given_name).to be_blank
          expect(cd_invite.invited_family_name).to be_blank
          expect(cd_invite.invited_phone).to be_blank
          expect(cd_invite.invited_email).to be_blank
        end
        it 'should redirect to organization page with notice' do
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to redirect_to(organization_path(org))
          expected_message =  "Invitation accepted. You can now manage this organization's credentials. Learn more."
          expect(flash[:notice]).to eq expected_message
        end
      end
      context 'failure' do
        it 'should show warning page with 404 if missing' do
          post "/organizations/#{org.id}/invitations/bad-id/confirm", params: success_params
          expect(response).to be_not_found
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page if cancelled' do
          cd_invite.update(status: :cancelled)
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page if expired' do
          cd_invite.update_attribute(:created_at, 3.days.ago)
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page if accepted' do
          cd_invite.accept!
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end

        it 'should show login if token expired' do
          user_service_class = class_double(UserInfoService).as_stubbed_const
          expect(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'unauthorized')
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to be_ok
          expect(response.body).to include(login_organization_invitation_path(org, cd_invite))
        end

        it 'should show error page if user info issue' do
          user_service_class = class_double(UserInfoService).as_stubbed_const
          expect(user_service_class).to receive(:new).and_raise(UserInfoServiceError, 'terrible thing happened')
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response.body).to include(I18n.t('verification.server_error_text'))
        end

        context 'not match' do
          before { stub_user_info }
          it 'should render form with error if OTP not match' do
            post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: fail_params
            expect(response).to be_bad_request
            expect(response.body).to include(confirm_organization_invitation_path(org, cd_invite))
          end
          it 'should render error page if family_name not match' do
            cd_invite.update_attribute(:invited_family_name, "not #{cd_invite.invited_family_name}")
            post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
            expect(response).to be_forbidden
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
            expect(response.body).to include('usa-alert--error')
          end
          it 'should render error page if phone not match' do
            cd_invite.update_attribute(:invited_phone, '9129999999')
            post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
            expect(response).to be_forbidden
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
            expect(response.body).to include('usa-alert--error')
          end
          it 'should render error page if PII not match and OTP not match' do
            cd_invite.update_attribute(:invited_family_name, "not #{cd_invite.invited_family_name}")
            post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: fail_params
            expect(response).to be_forbidden
            expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
            expect(response.body).to include('usa-alert--error')
          end
        end
      end
    end
    context :ao do
      let!(:ao_invite) { create(:invitation, :ao) }
      let(:user) do
        create(:user, given_name: 'Herman',
                      family_name: 'Wouk',
                      email: ao_invite.invited_email)
      end
      let(:org) { ao_invite.provider_organization }

      before do
        sign_in user
      end
      context 'success' do
        before do
          stub_user_info
        end
        it 'should create AoOrgLink' do
          expect do
            post "/organizations/#{org.id}/invitations/#{ao_invite.id}/confirm"
          end.to change { AoOrgLink.count }.by(1)
        end
        it 'should update invitation' do
          post "/organizations/#{org.id}/invitations/#{ao_invite.id}/confirm"
          ao_invite.reload
          expect(ao_invite.invited_given_name).to be_blank
          expect(ao_invite.invited_family_name).to be_blank
          expect(ao_invite.invited_phone).to be_blank
          expect(ao_invite.invited_email).to be_blank
        end
        it 'should redirect to organization page with notice' do
          post "/organizations/#{org.id}/invitations/#{ao_invite.id}/confirm"
          expect(response).to redirect_to(organization_path(org))
          expected_message =  'Invitation accepted.'
          expect(flash[:notice]).to eq expected_message
        end
      end
      context 'failure' do
        before do
          stub_user_info(overrides: { 'social_security_number' => '900111112' })
        end
        it 'should render error page if SSN not match' do
          ao_invite.update(invited_email: 'some-other-email@example.com')
          post "/organizations/#{org.id}/invitations/#{ao_invite.id}/confirm"
          expect(response).to be_forbidden
          expect(response.body).to_not include(confirm_organization_invitation_path(org, ao_invite))
          expect(response.body).to include('usa-alert--error')
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
      expect(response.body).to include('usa-alert--warning')
    end
  end
end

describe 'POST /renew' do
  let(:fail_message) { 'Unable to create new invitation' }
  context :ao do
    let!(:invitation) { create(:invitation, :ao) }
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

def user_info(overrides)
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
  expect(user_service_class).to receive(:new).and_return(user_service)

  expect(user_service).to receive(:user_info).and_return(user_info(overrides))
end
