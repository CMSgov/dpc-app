# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'CredentialDelegateInvitations', type: :request do
  include DpcClientSupport

  describe 'GET /new' do
    context 'not logged in' do
      it 'redirects to login' do
        get '/organizations/no-such-id/credential_delegate_invitations/new'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'as ao' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }

      before do
        create(:ao_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'returns success' do
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(assigns(:organization)).to eq org
        expect(response).to have_http_status(200)
      end

      it 'shows tos page if not signed' do
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(assigns(:organization)).to eq org
        expect(response.body).to include('<h2>Sign Terms of Service</h2>')
      end

      it 'shows invite cd form if tos signed' do
        org.update(terms_of_service_accepted_by: user)
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(assigns(:organization)).to eq org
        expect(response.body).to include('<h1>Invite new user</h1>')
      end
    end

    context 'user has sanctions' do
      let!(:user) { create(:user, verification_status: 'rejected', verification_reason: 'ao_med_sanctions') }
      let!(:org) { create(:provider_organization) }
      before { sign_in user }

      it 'should show access denied page' do
        create(:ao_org_link, provider_organization: org, user:)
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(response.body).to include(I18n.t('verification.ao_med_sanctions_status'))
        expect(assigns(:organization)).to be_nil
      end
    end

    context 'org has sanctions' do
      let!(:user) { create(:user) }
      let!(:org) do
        create(:provider_organization, terms_of_service_accepted_by: user, verification_status: 'rejected',
                                       verification_reason: 'org_med_sanctions')
      end
      before { sign_in user }

      it 'should show access denied page' do
        create(:ao_org_link, provider_organization: org, user:)
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(response.body).to include(I18n.t('verification.org_med_sanctions_status'))
      end
    end

    context 'org not approved' do
      let!(:user) { create(:user) }
      let!(:org) do
        create(:provider_organization, terms_of_service_accepted_by: user, verification_status: 'rejected',
                                       verification_reason: 'no_approved_enrollment')
      end
      before { sign_in user }

      it 'should show access denied page' do
        create(:ao_org_link, provider_organization: org, user:)
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(response.body).to include(I18n.t('verification.no_approved_enrollment_status'))
      end
    end

    context 'user no longer ao' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by: user) }
      before { sign_in user }

      it 'should show access denied page' do
        create(:ao_org_link, provider_organization: org, user:, verification_status: false,
                             verification_reason: 'user_not_authorized_official')
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(response.body).to include(I18n.t('verification.user_not_authorized_official_status'))
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }
      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end
      it 'redirects to organizations' do
        get "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(response).to redirect_to('/organizations')
      end
    end
  end

  describe 'POST /create' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, terms_of_service_accepted_by: user) }
    let!(:successful_parameters) do
      { invited_given_name: 'Bob',
        invited_family_name: 'Hodges',
        invited_email: 'bob@example.com',
        invited_email_confirmation: 'bob@example.com' }
    end

    context 'as ao' do
      let(:api_id) { org.id }
      before do
        create(:ao_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'creates invitation record on success' do
        expect do
          post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
        end.to change { Invitation.count }.by(1)
      end

      it 'redirects on success' do
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
        expect(response).to redirect_to(success_organization_credential_delegate_invitation_path(api_id,
                                                                                                 'new-invitation'))
      end

      it 'sends an invitation email on success' do
        mailer = double(InvitationMailer)
        expect(InvitationMailer).to receive(:with).with(invitation: instance_of(Invitation)).and_return(mailer)
        expect(mailer).to receive(:invite_cd).and_return(mailer)
        expect(mailer).to receive(:deliver_later)
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      end

      it 'logs on success' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['Credential Delegate invited',
                                                     { actionContext: LoggingConstants::ActionContext::Registration,
                                                       actionType: LoggingConstants::ActionType::CdInvited }])
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
      end

      it 'does not create invitation record on failure' do
        successful_parameters['invited_given_name'] = ''
        expect do
          post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
        end.to change { Invitation.count }.by(0)
      end

      it 'does not redirect on failure' do
        successful_parameters['invited_given_name'] = ''
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
        expect(response.status).to eq(400)
      end

      it 'does not create duplicate invitation record' do
        post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
        expect do
          post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
        end.to change { Invitation.count }.by(0)
        expect(response.status).to eq(400)
        expect(response.body).to include(I18n.t('verification.duplicate_status'))
        expect(response.body).to include(I18n.t('verification.duplicate_text'))
      end
    end

    context 'as cd' do
      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'fails even with good parameters' do
        expect do
          post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
        end.to change { Invitation.count }.by(0)
        expect(response).to redirect_to(organizations_path)
      end
    end
  end

  describe 'GET /success' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, terms_of_service_accepted_by: user) }

    before do
      sign_in user
      create(:ao_org_link, provider_organization: org, user:)
    end

    it 'returns success' do
      get "/organizations/#{org.id}/credential_delegate_invitations/foo/success"
      expect(assigns(:organization)).to eq org
      expect(response).to have_http_status(200)
    end
  end

  describe 'Delete /destroy' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, terms_of_service_accepted_by: user) }
    let!(:invitation) { create(:invitation, :cd, provider_organization: org) }

    context 'as cd' do
      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end
      it 'fails' do
        delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
        expect(invitation.reload).to be_pending
        expect(response).to redirect_to(organizations_path)
      end
    end

    context 'as ao' do
      before do
        create(:ao_org_link, provider_organization: org, user:)
        sign_in user
      end
      it 'soft deletes invitation' do
        expect do
          delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
        end.to change { Invitation.count }.by(0)
        expect(invitation.reload).to be_cancelled
        expect(response).to redirect_to(organization_path(org))
      end
      it 'flashes success if succeeds' do
        delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
        expect(flash[:notice]).to eq('Invitation cancelled.')
      end
      it 'returns error message on failure' do
        invitation_class = class_double(Invitation).as_stubbed_const
        bad_invitation = instance_double(Invitation)
        expect(bad_invitation).to receive(:provider_organization).and_return(org)
        expect(bad_invitation).to receive(:update).and_return(false)
        allow(bad_invitation).to receive_message_chain(:errors, :size).and_return(5)
        expect(bad_invitation).to receive_message_chain(:errors, :full_messages).and_return(%w[fake_error])
        expect(invitation_class).to receive(:find).with(invitation.id.to_s).and_return(bad_invitation)

        delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
        expect(flash[:alert]).to eq('fake_error')
        expect(invitation.reload).to be_pending
      end
      it 'does not allow deletion of invitation for another org' do
        other_org = create(:provider_organization)
        other_invitation = create(:invitation, :cd, provider_organization: other_org)
        delete "/organizations/#{org.id}/credential_delegate_invitations/#{other_invitation.id}"
        expect(other_invitation.reload).to be_pending
        expect(flash[:alert]).to eq('You do not have permission to cancel this invitation.')
        expect(response).to redirect_to(organization_path(org))
      end
      it 'does not allow deletion of accepted invitation' do
        invitation.accept!
        delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
        expect(invitation.reload).to be_accepted
        expect(flash[:alert]).to eq('You may not cancel an accepted invitation.')
        expect(response).to redirect_to(organization_path(org))
      end
    end
  end
end
