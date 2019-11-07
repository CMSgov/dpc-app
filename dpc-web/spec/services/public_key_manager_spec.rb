# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do
  describe '#create_public_key' do
    context 'with valid key' do
      context 'successful API request' do
        it 'responds true' do
          org = create(:organization)
          registered_org = create(:registered_organization, api_env: 0, organization: org)

          api_client = instance_double(APIClient)
          allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
          allow(api_client).to receive(:create_public_key)
            .with(registered_org.api_id, params: { label: 'Test Key 1', key: file_fixture("stubbed_cert.pem").read })
            .and_return(api_client)
          allow(api_client).to receive(:response_successful?).and_return(true)
          allow(api_client).to receive(:response_body).and_return({ 'id': '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' })

          manager = PublicKeyManager.new(api_env: 'sandbox', organization: registered_org.organization)
          expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture("stubbed_cert.pem").read)).to eq(true)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          org = create(:organization)
          registered_org = create(:registered_organization, api_env: 0, organization: org)

          api_client = instance_double(APIClient)
          allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
          allow(api_client).to receive(:create_public_key)
            .with(registered_org.api_id, params: { label: 'Test Key 1', key: file_fixture("stubbed_cert.pem").read })
            .and_return(api_client)
          allow(api_client).to receive(:response_successful?).and_return(false)

          manager = PublicKeyManager.new(api_env: 'sandbox', organization: registered_org.organization)
          expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture("stubbed_cert.pem").read)).to eq(false)
        end
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        org = create(:organization)
        registered_org = create(:registered_organization, api_env: 0, organization: org)
        manager = PublicKeyManager.new(api_env: 'sandbox', organization: registered_org.organization)

        expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture("private_key.pem").read)).to eq(false)
      end

      it 'returns false when key is not in pem format' do
        org = create(:organization)
        registered_org = create(:registered_organization, api_env: 0, organization: org)
        manager = PublicKeyManager.new(api_env: 'sandbox', organization: registered_org.organization)

        expect(manager.create_public_key(label: 'Test Key 1', public_key: file_fixture("bad_cert.pub").read)).to eq(false)
      end
    end
  end
end