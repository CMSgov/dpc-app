require 'rails_helper'

RSpec.describe RegisteredOrganization, type: :model do
  include APIClientSupport

  describe '#client_tokens' do
    it 'gets array from ClientTokenManager' do
      org = create(:organization)
      registered_org = build(:registered_organization, organization: org, api_env: 0)
      tokens = [{ 'token' => 'abcdef' }, { 'token' => 'ftguiol' }]

      manager = instance_double(ClientTokenManager)
      allow(ClientTokenManager).to receive(:new).with(api_env: 'sandbox', registered_organization: registered_org)
                                                .and_return(manager)
      allow(manager).to receive(:client_tokens).and_return(tokens)

      expect(registered_org.client_tokens).to eq(tokens)
    end
  end

  describe '#public_keys' do
    it 'gets array from PublicKeyManager' do
      org = create(:organization)
      registered_org = build(:registered_organization, organization: org, api_env: 0)
      keys = [{'id' => 'abcdef'}, {'id' => 'ftguiol'}]

      manager = instance_double(PublicKeyManager)
      allow(PublicKeyManager).to receive(:new).with(api_env: 'sandbox', registered_organization: registered_org)
                                                .and_return(manager)
      allow(manager).to receive(:public_keys).and_return(keys)

      expect(registered_org.public_keys).to eq(keys)
    end
  end

  describe 'callbacks' do
    describe '#create_api_organization' do
      it 'invokes APIClient and returns the response body' do
        api_client = stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        reg_org = create(:registered_organization)

        expect(api_client).to have_received(:create_organization)
          .with(reg_org.organization, fhir_endpoint: reg_org.fhir_endpoint.attributes.slice('name', 'status', 'uri'))
          .once
        expect(reg_org.create_api_organization).to eq(default_org_creation_response)
      end

      context 'successful API response' do
        it 'updates attributes and notifies users' do
          stub_api_client(
            message: :create_organization,
            success: true,
            response: {
              'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad',
              'endpoint' => [
                'reference' => 'Endpoint/437f7b17-3d48-4654-949d-57ea80f8f1d7'
              ]
            }
          )
          allow_any_instance_of(Organization).to receive(:notify_users_of_sandbox_access)

          reg_org = create(:registered_organization, api_endpoint_ref: nil, api_id: nil)

          expect(reg_org.organization).to have_received(:notify_users_of_sandbox_access).once
          expect(reg_org.api_id).to eq('923a4f7b-eade-494a-8ca4-7a685edacfad')
          expect(reg_org.api_endpoint_ref).to eq('Endpoint/437f7b17-3d48-4654-949d-57ea80f8f1d7')
        end
      end

      context 'unsuccessful API response'do
        it 'does not notify users and adds to errors' do
          stub_api_client(message: :create_organization, success: false, response: { 'issues' => ['Bad Request'] })

          reg_org = build(:registered_organization, api_endpoint_ref: nil, api_id: nil)
          reg_org.build_default_fhir_endpoint
          reg_org.save

          expect(reg_org.api_id).to be_nil
          expect(reg_org.api_endpoint_ref).to be_nil
          expect(reg_org.errors.count).to eq(1)
          expect(reg_org).not_to be_persisted
        end
      end
    end

    describe '#notify_users_of_sandbox_access' do
      context 'when sandbox' do
        it 'tells organization to notify users' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          expect_any_instance_of(Organization).to receive(:notify_users_of_sandbox_access)

          reg_org = create(:registered_organization, api_env: 'sandbox')
        end
      end

      context 'when production' do
        it 'does not tell organization to notify users' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          expect_any_instance_of(Organization).not_to receive(:notify_users_of_sandbox_access)

          reg_org = create(:registered_organization, api_env: 'production')
        end
      end
    end

    describe '#update_api_organization' do
      context 'successful API request' do
        it 'makes update API request and does not add to errors' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          reg_org = create(:registered_organization)

          api_client = stub_api_client(message: :update_organization, success: true, response: default_org_creation_response)
          allow(reg_org).to receive(:update_api_endpoint)
          reg_org.update(updated_at: Time.now)

          expect(api_client).to have_received(:update_organization).with(reg_org)
        end
      end

      context 'failed API request' do
        it 'adds to errors' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          reg_org = create(:registered_organization)

          api_client = stub_api_client(message: :update_organization, success: false, response: 'Bad error')
          allow(reg_org).to receive(:update_api_endpoint)
          reg_org.update(updated_at: Time.now)

          expect(api_client).to have_received(:update_organization).with(reg_org)
          expect(reg_org.errors.count).to eq(1)
        end
      end
    end

    describe '#update_api_endpoint' do
      context 'successful API request' do
        it 'makes update API request before update and updates object' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          old_attr = 1.day.ago
          new_attr = Time.now
          reg_org = create(:registered_organization, updated_at: old_attr)

          api_client = stub_api_client(message: :update_endpoint, success: true, response: default_org_creation_response)
          allow(reg_org).to receive(:update_api_organization)
          reg_org.update(updated_at: new_attr)

          expect(api_client).to have_received(:update_endpoint).with(reg_org)
          expect(reg_org.updated_at).to eq(new_attr)
        end
      end

      context 'failed API request' do
        it 'adds to errors and does not update object' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          old_attr = 1.day.ago
          new_attr = Time.now
          reg_org = create(:registered_organization, updated_at: old_attr)

          api_client = stub_api_client(message: :update_endpoint, success: false, response: { issues: ['Bad request'] })
          allow(reg_org).to receive(:update_api_organization)
          reg_org.update(updated_at: new_attr)

          expect(api_client).to have_received(:update_endpoint).with(reg_org)
          expect(reg_org.errors.count).to eq(1)
          expect(reg_org.reload.updated_at).to eq(old_attr)
        end
      end
    end

    describe '#delete_api_organization' do
      context 'successful API request' do
        it 'makes update API request before destroy and destroys object' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          reg_org = create(:registered_organization)

          api_client = stub_api_client(message: :delete_organization, success: true, response: '')

          reg_org.destroy

          expect(api_client).to have_received(:delete_organization).with(reg_org)
          expect{ reg_org.reload }.to raise_error(ActiveRecord::RecordNotFound)
        end
      end

      context 'failed API request' do
        it 'adds to errors and does not destroy object' do
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          reg_org = create(:registered_organization)

          api_client = stub_api_client(message: :delete_organization, success: false, response: '')
          expect(
            reg_org.destroy
          ).to eq(false)

          expect(api_client).to have_received(:delete_organization).with(reg_org)
          expect(reg_org.errors.count).to eq(1)
          expect(reg_org.reload).to eq(reg_org)
        end
      end
    end
  end

  describe '#build_default_fhir_endpoint' do
    it 'builds fhir_endpoint with default attributes' do
      reg_org = build(:registered_organization)

      reg_org.build_default_fhir_endpoint
      fhir_endpoint = reg_org.fhir_endpoint

      expect([fhir_endpoint.name, fhir_endpoint.status, fhir_endpoint.uri])
        .to eq(['DPC Sandbox Test Endpoint', 'test', 'https://dpc.cms.gov/test-endpoint'])
    end
  end
end
