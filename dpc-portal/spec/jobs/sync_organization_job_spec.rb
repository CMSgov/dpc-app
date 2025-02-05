# frozen_string_literal: true

require 'rails_helper'

RSpec.describe SyncOrganizationJob, type: :job do
  include ActiveJob::TestHelper

  let(:mock_dpc_client) { instance_double(DpcClient) }
  let(:create_organization_success_response) do
    MockOrgResponse.new(response_successful: true, response_body: {
                          'resourceType' => 'Organization',
                          'id' => '352dda55-6925-4bdb-bcee-1af0bc163699'
                        })
  end
  let(:create_organization_unsuccessful_response) { MockOrgResponse.new(response_successful: false, response_body: {}) }
  let(:get_organization_zero_entries) { MockFHIRResponse.new(entries_count: 0) }
  let(:get_organization_one_entry) { MockFHIRResponse.new(entries_count: 1) }
  let(:get_organization_two_entries) { MockFHIRResponse.new(entries_count: 2) }

  let(:provider_organization) do
    create(
      :provider_organization,
      name: 'Test',
      dpc_api_organization_id: 'foo'
    )
  end

  before(:each) do
    SyncOrganizationJob.perform_later(provider_organization.id)
  end

  before do
    allow(DpcClient).to receive(:new).and_return(mock_dpc_client)
  end

  describe 'perform' do
    it 'creates an org in dpc-api if api_client returns no entry, then updates api org id' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [provider_organization.id])
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(provider_organization.npi)
                                                                  .and_return(get_organization_zero_entries)
      expect(mock_dpc_client).to receive(:create_organization)
        .with(have_attributes(
                name: provider_organization.name,
                npi: provider_organization.npi
              ))
        .and_return(create_organization_success_response)

      perform_enqueued_jobs
    end

    it 'updates provider_organization.npi if api_client returns an entry' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [provider_organization.id])
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(provider_organization.npi)
                                                                  .and_return(get_organization_one_entry)

      perform_enqueued_jobs

      provider_organization.reload
      expected_dpc_api_organization_id = get_organization_one_entry.entry[0].resource.id.to_s
      expect(provider_organization.dpc_api_organization_id).to eq expected_dpc_api_organization_id
    end

    it 'raises an error if multiple orgs are found for the NPI in dpc_attribution' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [provider_organization.id])
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(provider_organization.npi)
                                                                  .and_return(get_organization_two_entries)
      expect do
        SyncOrganizationJob.perform_now(provider_organization.id)
      end.to raise_error(SyncOrganizationJobError,
                         "multiple orgs found for NPI #{provider_organization.npi} in dpc_attribution")
    end

    it 'raises an error if api_client.create_organization is unsuccessful' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [provider_organization.id])
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(provider_organization.npi)
                                                                  .and_return(get_organization_zero_entries)
      expect(mock_dpc_client).to receive(:create_organization)
        .with(have_attributes(
                name: provider_organization.name,
                npi: provider_organization.npi
              ))
        .and_return(create_organization_unsuccessful_response)

      expect do
        SyncOrganizationJob.perform_now(provider_organization.id)
      end.to raise_error(SyncOrganizationJobError,
                         "DpcClient.create_organization failed for provider_organization #{provider_organization.id}")
    end

    it 'raises an error if the provided unique ID is not found' do
      provider_organization_id = provider_organization.id
      provider_organization.destroy
      assert_enqueued_with(job: SyncOrganizationJob, args: [provider_organization_id])
      expect(ProviderOrganization).to receive(:find)
        .with(provider_organization_id)
        .and_raise(ActiveRecord::RecordNotFound)
      expect do
        SyncOrganizationJob.perform_now(provider_organization_id)
      end.to raise_error(SyncOrganizationJobError, "provider_organization #{provider_organization_id} not found")
    end
  end
end

class MockFHIRResponse
  attr_reader :entry

  def initialize(entries_count: 0)
    @entry = entries_count.times.map { MockEntry.new }
  end
end

class MockEntry
  attr_reader :resource

  def initialize
    @resource = MockResource.new
  end
end

class MockResource
  attr_reader :id

  def initialize
    @id = rand(0..9)
  end
end

class MockOrgResponse
  attr_reader :response_body

  def initialize(response_successful: true, response_body: {})
    @response_successful = response_successful
    @response_body = response_body
  end

  def response_successful?
    @response_successful
  end
end
