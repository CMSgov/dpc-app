# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe CdOrgLinksController, type: :request do
  include LoginSupport

  let(:provider) { :login_dot_gov }
  let(:organization) { create(:provider_organization) }
  let(:ao_user) { create_user_with_csp(csp: provider, given_name: 'Bob', family_name: 'Hoskins') }
  let(:cd_user) { create_user_with_csp(csp: provider, given_name: 'Lisa', family_name: 'Franklin') }
  let(:invitation) { create(:invitation, :cd, provider_organization: organization, invited_by: ao_user) }
  let(:cd_org_link) do
    create(:cd_org_link, user: cd_user, provider_organization: organization, invitation: invitation)
  end

  describe 'DELETE /organizations/:organization_id/cd_org_links/:id' do
    context 'user is signed in' do
      before do
        sign_in ao_user, csp: provider
      end

      context 'when the AO is authorized' do
        before do
          create(:ao_org_link, user: ao_user, provider_organization: organization)
          cd_org_link
        end

        it 'destroys the CdOrgLink' do
          expect do
            delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          end.to change { CdOrgLink.count }.by(-1)
        end

        it 'shows a success flash' do
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          expect(flash[:success]).to eq('Successfully removed Credential Delegate.')
        end

        it 'redirects to the organization page' do
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          expect(response).to redirect_to(organization_path(organization))
        end

        it 'logs the CD removal' do
          expect_any_instance_of(described_class).to receive(:log_event)
            .with(:info, 'Credential Delegate removed from organization', hash_including(action_context: LoggingConstants::ActionContext::Registration,
                                                                                         action_type: LoggingConstants::ActionType::CdRemovedFromOrg,
                                                                                         csp: provider.to_s))
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
        end
      end

      context 'when the CD belongs to a different organization' do
        let(:other_organization) { create(:provider_organization) }
        let(:other_cd_org_link) do
          create(:cd_org_link, user: cd_user, provider_organization: other_organization, invitation: invitation)
        end

        before do
          create(:ao_org_link, user: ao_user, provider_organization: organization)
          cd_org_link
        end

        it 'raises a record not found error' do
          expect do
            delete "/organizations/#{organization.path_id}/cd_org_links/#{other_cd_org_link.id}"
          end.to raise_error(ActiveRecord::RecordNotFound)
        end
      end

      context 'when the user is not an AO for the organization' do
        before do
          cd_org_link
        end

        it 'does not destroy the CdOrgLink' do
          expect do
            delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          end.to change { CdOrgLink.count }.by(0)
        end

        it 'redirects with an error' do
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          expect(response).to have_http_status(:redirect)
        end
      end

      context 'when the destruction fails' do
        before do
          create(:ao_org_link, user: ao_user, provider_organization: organization)
          cd_org_link
          allow_any_instance_of(CdOrgLink).to receive(:destroy!).and_raise(ActiveRecord::RecordNotDestroyed)
        end

        it 'shows an alert on failure' do
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          expect(flash[:alert]).to eq('Failed to remove Credential Delegate. Please try again later.')
        end

        it 'redirects to the organization page' do
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          expect(response).to redirect_to(organization_path(organization))
        end

        it 'logs the CD non-removal' do
          expect_any_instance_of(described_class).to receive(:log_event)
            .with(:error, 'Credential Delegate not removed from organization', hash_including(action_context: LoggingConstants::ActionContext::Registration,
                                                                                              action_type: LoggingConstants::ActionType::CdNotRemovedFromOrg,
                                                                                              csp: provider.to_s))
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
        end

        it 'does not destroy the CdOrgLink' do
          expect do
            delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
          end.to change { CdOrgLink.count }.by(0)
        end
      end
    end

    context 'when the user is not signed in' do
      it 'redirects to the sign in page' do
        delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
        expect(response).to redirect_to(sign_in_path)
      end

      it 'does not destroy the CdOrgLink' do
        cd_org_link
        expect do
          delete "/organizations/#{organization.path_id}/cd_org_links/#{cd_org_link.id}"
        end.to change { CdOrgLink.count }.by(0)
      end
    end
  end
end
