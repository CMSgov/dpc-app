require 'rails_helper'

RSpec.describe RegisteredOrganization, type: :model do
  describe '#client_tokens' do
    it 'gets array from ClientTokenManager' do
      org = create(:organization)
      registered_org = create(:registered_organization, organization: org, api_env: 0)
      tokens = [{'token' => 'abcdef'}, {'token' => 'ftguiol'}]

      manager = instance_double(ClientTokenManager)
      allow(ClientTokenManager).to receive(:new).with(api_env: 'sandbox', organization: org)
                                                .and_return(manager)
      allow(manager).to receive(:client_tokens).and_return(tokens)

      expect(registered_org.client_tokens).to eq(tokens)
    end
  end
end
