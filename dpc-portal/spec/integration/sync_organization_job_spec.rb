# frozen_string_literal: true

require 'rails_helper'

RSpec.describe SyncOrganizationJob, type: :job do
  before(:example) { WebMock.disable_net_connect!(allow_localhost: true, allow: ['api']) }
  after(:example) { WebMock.disable_net_connect!(allow_localhost: true) }
  describe 'Sync Organization', :integration do
    let(:npi) { Luhnacy.generate(15, prefix: '808403')[-10..] }
    let(:blocker) { 'foo' }
    let(:provider_organization) do
      create(
        :provider_organization,
        name: 'Test',
        npi:,
        dpc_api_organization_id: blocker
      )
    end
    it 'should create a new organization in the API' do
      api_client = DpcClient.new

      api_response = api_client.get_organization_by_npi(npi)
      expect(api_response.entry).to be_empty
      expect(provider_organization.dpc_api_organization_id).to eq blocker

      SyncOrganizationJob.perform_now(provider_organization.id)

      provider_organization.reload

      api_response = api_client.get_organization_by_npi(npi)
      expect(api_response.entry).to_not be_empty

      org_id = api_response.entry.first.resource.id
      expect(provider_organization.dpc_api_organization_id).to eq org_id

      org_name = api_response.entry.first.resource.name
      expect(provider_organization.name).to eq org_name
    end
  end
end
