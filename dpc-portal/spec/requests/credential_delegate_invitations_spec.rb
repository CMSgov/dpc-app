# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe 'CredentialDelegateInvitations', type: :request do
  include DpcClientSupport
  include LoginSupport

  let(:terms_of_service_accepted_by) { nil }

  RSpec.shared_context 'as ao' do |provider|
    let!(:user) { create_user_with_csp(provider) }
    let!(:org)  { create(:provider_organization) }

    before do
      create(:ao_org_link, provider_organization: org, user:)
      sign_in user, csp: provider
    end
  end

  RSpec.shared_context 'as cd' do |provider|
    let!(:user) { create_user_with_csp(provider) }
    let!(:org)  { create(:provider_organization) }

    before do
      create(:cd_org_link, provider_organization: org, user:)
      sign_in user, csp: provider
    end
  end

  LoginSupport::CSP_MAP.each do |provider, display_name|
    context "using #{display_name}" do
      describe 'GET /new' do
        new_path = ->(org) { "/organizations/#{org.id}/credential_delegate_invitations/new" }
        context 'not logged in' do
          it 'redirects to login' do
            get '/organizations/no-such-id/credential_delegate_invitations/new'
            expect(response).to redirect_to('/users/sign_in')
          end
        end
        context 'as ao' do
          include_context 'as ao', provider

          it 'returns success' do
            get "/organizations/#{org.id}/credential_delegate_invitations/new"
            expect(assigns(:organization)).to eq org
            expect(response).to have_http_status(200)
          end

          it 'shows tos page if not signed' do
            get "/organizations/#{org.id}/credential_delegate_invitations/new"
            expect(assigns(:organization)).to eq org
            expect(response.body).to include('<h1>Sign Terms of Service</h1>')
          end

          it 'shows invite cd form if tos signed' do
            org.update(terms_of_service_accepted_by: user)
            get "/organizations/#{org.id}/credential_delegate_invitations/new"
            expect(assigns(:organization)).to eq org
            expect(response.body).to include('<h1>Assign Credential Delegate</h1>')
          end
        end
        context 'as cd' do
          include_context 'as cd', provider

          it 'redirects to organizations' do
            get "/organizations/#{org.id}/credential_delegate_invitations/new"
            expect(response).to redirect_to('/organizations')
          end
        end
        context 'ao access denied' do
          context 'user has sanctions' do
            it_behaves_like 'ao access denied with user sanctions', provider, new_path
          end
          context 'org has sanctions' do
            it_behaves_like 'ao access denied with org sanctions', provider, new_path
          end
          context 'org not approved' do
            it_behaves_like 'ao access denied with org not approved', provider, new_path
          end
          context 'user no longer ao' do
            it_behaves_like 'ao access denied user no longer ao', provider, new_path
          end
        end
      end
      describe 'POST /create' do
        context 'as ao' do
          let!(:user) { create_user_with_csp(provider) }
          let!(:org)  { create(:provider_organization, terms_of_service_accepted_by: user) }
          let!(:successful_parameters) do
            { invited_given_name: 'Bob',
              invited_family_name: 'Hodges',
              invited_email: 'bob@example.com',
              invited_email_confirmation: 'bob@example.com' }
          end
          let(:api_id) { org.id }

          before do
            create(:ao_org_link, provider_organization: org, user:)
            sign_in user, csp: provider
          end

          it 'creates invitation record on success' do
            expect do
              post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
            end.to change(Invitation, :count).by(1)
          end

          it 'redirects on success' do
            post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
            expect(flash[:success]).to eq('Credential Delegate invited successfully.')
            expect(response).to redirect_to(organization_path(org.id))
          end

          it 'sends an invitation email on success' do
            mailer = double(InvitationMailer)
            expect(InvitationMailer).to receive(:with).with(invitation: instance_of(Invitation)).and_return(mailer)
            expect(mailer).to receive(:invite_cd).and_return(mailer)
            expect(mailer).to receive(:deliver_later)
            post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
          end

          it 'logs on success' do
            invitation_id = 123
            invitation    = instance_double(Invitation)
            expect(invitation).to receive(:id).and_return(invitation_id)
            expect(invitation).to receive(:save).and_return(true)
            expect(Invitation).to receive(:new).and_return(invitation)

            mailer = double(InvitationMailer)
            expect(InvitationMailer).to receive(:with).with(invitation:).and_return(mailer)
            expect(mailer).to receive(:invite_cd).and_return(mailer)
            expect(mailer).to receive(:deliver_later)

            allow(Rails.logger).to receive(:info)
            expect(Rails.logger).to receive(:info).with(
              ['Credential Delegate invited',
               { actionContext: LoggingConstants::ActionContext::Registration,
                 actionType: LoggingConstants::ActionType::CdInvited,
                 invitation: invitation_id }]
            )
            post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
          end

          it 'does not create invitation record on failure' do
            successful_parameters['invited_given_name'] = ''
            expect do
              post "/organizations/#{api_id}/credential_delegate_invitations", params: successful_parameters
            end.to change(Invitation, :count).by(0)
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
            end.to change(Invitation, :count).by(0)
            expect(response.status).to eq(400)
            expect(response.body).to include(I18n.t('errors.attributes.base.duplicate_cd.status'))
            expect(response.body).to include(I18n.t('errors.attributes.base.duplicate_cd.text'))
          end
        end
        context 'as cd' do
          let!(:user) { create_user_with_csp(provider) }
          let!(:org)  { create(:provider_organization, terms_of_service_accepted_by: user) }
          let!(:successful_parameters) do
            { invited_given_name: 'Bob',
              invited_family_name: 'Hodges',
              invited_email: 'bob@example.com',
              invited_email_confirmation: 'bob@example.com' }
          end

          before do
            create(:cd_org_link, provider_organization: org, user:)
            sign_in user, csp: provider
          end

          it 'fails even with good parameters' do
            expect do
              post "/organizations/#{org.id}/credential_delegate_invitations", params: successful_parameters
            end.to change(Invitation, :count).by(0)
            expect(response).to redirect_to(organizations_path)
          end
        end
      end
      describe 'DELETE /destroy' do
        context 'as cd' do
          let!(:user) { create_user_with_csp(provider) }
          let!(:org)        { create(:provider_organization, terms_of_service_accepted_by: user) }
          let!(:invitation) { create(:invitation, :cd, provider_organization: org) }

          before do
            create(:cd_org_link, provider_organization: org, user:)
            sign_in(user, csp: provider)
          end

          it 'fails' do
            delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
            expect(invitation.reload).to be_pending
            expect(response).to redirect_to(organizations_path)
          end
        end
        context 'as ao' do
          let!(:user) { create_user_with_csp(provider) }
          let!(:org)        { create(:provider_organization, terms_of_service_accepted_by: user) }
          let!(:invitation) { create(:invitation, :cd, provider_organization: org) }

          before do
            create(:ao_org_link, provider_organization: org, user:)
            sign_in(user, csp: provider)
          end

          it 'soft deletes invitation' do
            expect do
              delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
            end.to change(Invitation, :count).by(0)
            expect(invitation.reload).to be_cancelled
            expect(response).to redirect_to(organization_path(org))
          end

          it 'flashes success if succeeds' do
            delete "/organizations/#{org.id}/credential_delegate_invitations/#{invitation.id}"
            expect(flash[:success]).to eq('Credential Delegate invitation cancelled successfully.')
          end

          it 'returns error message on failure' do
            invitation_class = class_double(Invitation).as_stubbed_const
            bad_invitation   = instance_double(Invitation)
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
            other_org        = create(:provider_organization)
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
  end
end
