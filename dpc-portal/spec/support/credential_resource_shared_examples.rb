# frozen_string_literal: true

require 'spec_helper'

RSpec.shared_examples 'a credential resource' do
  describe 'Post /create' do
    context 'as cd' do
      let!(:user) { create(:user) }
      let(:org_api_id) { SecureRandom.uuid }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by:, dpc_api_organization_id: org_api_id) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end
      it 'adds a credential audit log record on success' do
        token_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: "create_#{credential}",
                                       response: send("default_get_#{credential.pluralize}",
                                                      guid: token_guid)['entities'].first,
                                       api_client:)
        expect do
          post "/organizations/#{org.id}/#{credential.pluralize}", params: create_params
        end.to change { CredentialAuditLog.count }.by 1
        log = CredentialAuditLog.last
        expect(log.user).to eq user
        expect(log.credential_type).to eq credential
        expect(log.dpc_api_credential_id).to eq token_guid
        expect(log.action).to eq 'add'
      end

      it 'does not add a credential audit log record on failure' do
        expect do
          post "/organizations/#{org.id}/#{credential.pluralize}"
        end.to change { CredentialAuditLog.count }.by 0
      end
    end
  end

  describe 'Delete /destroy' do
    context 'as cd' do
      let!(:user) { create(:user) }
      let(:org_api_id) { SecureRandom.uuid }
      let!(:org) { create(:provider_organization, terms_of_service_accepted_by:, dpc_api_organization_id: org_api_id) }

      before do
        create(:cd_org_link, provider_organization: org, user:)
        sign_in user
      end

      it 'adds a credential audit log record on success' do
        token_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: "delete_#{credential}",
                                       response: nil,
                                       with: [org_api_id, token_guid],
                                       api_client:)
        expect do
          delete "/organizations/#{org.id}/#{credential.pluralize}/#{token_guid}"
        end.to change { CredentialAuditLog.count }.by 1
        log = CredentialAuditLog.last
        expect(log.user).to eq user
        expect(log.credential_type).to eq credential
        expect(log.dpc_api_credential_id).to eq token_guid
        expect(log.action).to eq 'remove'
      end

      it 'does not add a credential audit log record on failure' do
        token_guid = SecureRandom.uuid
        api_client = stub_api_client(message: :get_organization,
                                     response: default_get_org_response(org_api_id))
        stub_self_returning_api_client(message: "delete_#{credential}",
                                       response: nil,
                                       success: false,
                                       with: [org_api_id, token_guid],
                                       api_client:)
        expect do
          delete "/organizations/#{org.id}/#{credential.pluralize}/#{token_guid}"
        end.to change { CredentialAuditLog.count }.by 0
      end
    end
  end
end
