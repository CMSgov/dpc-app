# frozen_string_literal: true

require 'rails_helper'

RSpec.describe RegisteredOrganization, type: :model do
  include DpcClientSupport

  describe '#client_tokens' do
    it 'gets array from ClientTokenManager' do
      org = create(:organization)
      registered_org = build(:registered_organization, organization: org)
      tokens = [{ 'token' => 'abcdef' }, { 'token' => 'ftguiol' }]

      manager = instance_double(ClientTokenManager)
      allow(ClientTokenManager).to receive(:new).with(registered_organization: registered_org)
                                                .and_return(manager)
      allow(manager).to receive(:client_tokens).and_return(tokens)

      expect(registered_org.client_tokens).to eq(tokens)
    end
  end

  describe '#public_keys' do
    it 'gets array from PublicKeyManager' do
      org = create(:organization)
      registered_org = build(:registered_organization, organization: org)
      keys = [{ 'id' => 'abcdef' }, { 'id' => 'ftguiol' }]

      manager = instance_double(PublicKeyManager)
      allow(PublicKeyManager).to receive(:new).with(registered_organization: registered_org)
                                              .and_return(manager)
      allow(manager).to receive(:public_keys).and_return(keys)

      expect(registered_org.public_keys).to eq(keys)
    end
  end

  describe 'callbacks' do
    describe '#create_api_organization' do
      before do
        # stub default value
        allow(ENV).to receive(:[]).and_call_original
      end

      it 'invokes ApiClient and returns the response body' do
        api_client = stub_api_client(
          message: :create_organization,
          success: true,
          response: default_org_creation_response
        )

        reg_org = create(:registered_organization)

        expect(api_client).to have_received(:create_organization).with(reg_org.organization).once
        expect(reg_org.create_api_organization).to eq(default_org_creation_response)
      end

      context 'successful API response' do
        it 'updates attributes and notifies users' do
          allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')

          stub_api_client(
            message: :create_organization,
            success: true,
            response: {
              'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad'
            }
          )
          allow_any_instance_of(Organization).to receive(:notify_users_of_sandbox_access)

          reg_org = create(:registered_organization, api_id: nil)

          expect(reg_org.organization).to have_received(:notify_users_of_sandbox_access).once
          expect(reg_org.api_id).to eq('923a4f7b-eade-494a-8ca4-7a685edacfad')
        end
      end

      context 'unsuccessful API response' do
        it 'does not notify users and adds to errors' do
          stub_api_client(message: :create_organization, success: false, response: { 'issues' => ['Bad Request'] })

          org = create(:organization)

          reg_org = build(:registered_organization, api_id: nil, organization: org)
          reg_org.save

          expect(reg_org.api_id).to be_nil
          expect(reg_org.errors.count).to eq(1)
          expect(reg_org).not_to be_persisted
        end
      end
    end

    describe '#notify_users_of_sandbox_access' do
      before do
        # stub default value
        allow(ENV).to receive(:[]).and_call_original
      end

      context 'when sandbox' do
        it 'tells organization to notify users' do
          allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')

          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          expect_any_instance_of(Organization).to receive(:notify_users_of_sandbox_access)

          create(:registered_organization)
        end
      end

      context 'when production' do
        it 'does not tell organization to notify users' do
          allow(ENV).to receive(:[]).with('ENV').and_return('production')
          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
          expect_any_instance_of(Organization).not_to receive(:notify_users_of_sandbox_access)

          create(:registered_organization)

          # DatabaseCleaner prohibits running DatabaseCleaner.clean to run in `production` env.
          # So the ENV call has to, once again, return test
          allow(ENV).to receive(:[]).with('ENV').and_return('test')
        end
      end
    end

    describe '#update_api_organization' do
      context 'successful API request' do
        it 'makes update API request and does not add to errors' do
          api_client = stub_api_client(
            message: :create_organization,
            success: true,
            response: default_org_creation_response
          )
          reg_org = create(:registered_organization)

          api_client = stub_api_client(
            api_client:,
            message: :update_organization,
            success: true,
            response: default_org_creation_response
          )
          reg_org.update(updated_at: Time.now)

          expect(api_client).to have_received(:update_organization).with(reg_org.organization, reg_org.api_id)
        end
      end

      context 'failed API request' do
        it 'adds to errors' do
          api_client = stub_api_client(
            message: :create_organization,
            success: true,
            response: default_org_creation_response
          )
          reg_org = create(:registered_organization)

          api_client = stub_api_client(
            api_client:,
            message: :update_organization,
            success: false,
            response: 'Bad error'
          )
          reg_org.update(updated_at: Time.now)

          expect(api_client).to have_received(:update_organization).with(reg_org.organization, reg_org.api_id)
          expect(reg_org.errors.count).to eq(1)
        end
      end
    end
  end
end
