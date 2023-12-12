# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  include DpcClientSupport

  it 'loads name on init' do
    api_id = SecureRandom.uuid
    stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
    organization = Organization.new(api_id)
    expect(organization.name).to eq "Bob's Health Hut"
  end

  it 'fetches public keys' do
    api_id = SecureRandom.uuid
    stub_client = stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
    stub_self_returning_api_client(message: :get_public_keys, response: default_get_public_keys,
                                   api_client: stub_client)
    organization = Organization.new(api_id)
    expect(organization.public_keys.size).to eq 1
    expect(organization.public_keys.first['id']).to eq '579dd199-3c2d-48e8-8594-cec35e223527'
  end

  it 'fetches client tokens' do
    api_id = SecureRandom.uuid
    stub_client = stub_api_client(message: :get_organization, response: default_get_org_response(api_id))
    stub_self_returning_api_client(message: :get_client_tokens, response: default_get_client_tokens,
                                   api_client: stub_client)
    organization = Organization.new(api_id)
    expect(organization.client_tokens.size).to eq 1
    expect(organization.client_tokens.first['id']).to eq 'bd49166a-f896-400f-aaa2-c6fa953e1128'
  end
end
