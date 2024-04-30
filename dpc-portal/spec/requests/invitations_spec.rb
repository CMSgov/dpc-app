# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Invitations', type: :request do
  describe 'GET /accept' do
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
          cd_invite.update_attribute(:cancelled_at, 3.days.ago)
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page if expired' do
          cd_invite.update_attribute(:created_at, 3.days.ago)
          get "/organizations/#{org.id}/invitations/#{cd_invite.id}/accept"
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should show warning page if accepted' do
          create(:cd_org_link, invitation: cd_invite)
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

      before do
        sign_in user
      end
      context 'success' do
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
          cd_invite.update_attribute(:cancelled_at, 3.days.ago)
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
          create(:cd_org_link, invitation: cd_invite)
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to be_forbidden
          expect(response.body).to include('usa-alert--warning')
        end
        it 'should render form with error if OTP not match' do
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: fail_params
          expect(response).to be_bad_request
          expect(response.body).to include(confirm_organization_invitation_path(org, cd_invite))
        end
        it 'should render error page if PII not match' do
          user.update_attribute(:family_name, "not #{cd_invite.invited_family_name}")
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: success_params
          expect(response).to be_forbidden
          expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
          expect(response.body).to include('usa-alert--error')
        end
        it 'should render error page if PII not match and OTP not match' do
          user.update_attribute(:family_name, "not #{cd_invite.invited_family_name}")
          post "/organizations/#{org.id}/invitations/#{cd_invite.id}/confirm", params: fail_params
          expect(response).to be_forbidden
          expect(response.body).to_not include(confirm_organization_invitation_path(org, cd_invite))
          expect(response.body).to include('usa-alert--error')
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
        it 'should render error page if PII not match' do
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
