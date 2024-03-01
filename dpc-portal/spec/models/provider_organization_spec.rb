# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ProviderOrganization, type: :model do
  include DpcClientSupport

  describe :validations do
    it 'should pass if it has an npi' do
      expect(ProviderOrganization.new(npi: '1111111111')).to be_valid
    end

    it 'should faile without npi' do
      expect(ProviderOrganization.new).to_not be_valid
    end
  end

  describe 'api interactions' do
    let(:api_id) { SecureRandom.uuid }
    let(:organization) { build(:provider_organization, dpc_api_organization_id: api_id) }

    it 'fetches api org' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
      expect(organization.api_org.name).to eq "Bob's Health Hut"
    end

    it 'throws error if no api org' do
      stub_api_client(message: :get_organization, response: nil)
      expect { organization.api_org }.to raise_error(DpcRecordNotFound)
    end

    it 'fetches public keys' do
      stub_client = stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
      stub_self_returning_api_client(message: :get_public_keys, response: default_get_public_keys,
                                     api_client: stub_client)
      expect(organization.public_keys.size).to eq 1
      expect(organization.public_keys.first['id']).to eq '579dd199-3c2d-48e8-8594-cec35e223527'
    end

    it 'fetches client tokens' do
      stub_client = stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
      stub_self_returning_api_client(message: :get_client_tokens, response: default_get_client_tokens,
                                     api_client: stub_client)
      expect(organization.client_tokens.size).to eq 1
      expect(organization.client_tokens.first['id']).to eq 'bd49166a-f896-400f-aaa2-c6fa953e1128'
    end

    it 'fetches ips' do
      stub_client = stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
      stub_self_returning_api_client(message: :get_ip_addresses, response: default_get_ip_addresses,
                                     api_client: stub_client)
      expect(organization.ip_addresses.size).to eq 1
      expect(organization.ip_addresses.first['id']).to eq '579dd199-3c2d-48e8-8594-cec35e223528'
    end
  end
end
