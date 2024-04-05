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

  let(:fhir_endpoint) do
    {
      'status' => 'test',
      'name' => 'Test Endpoint',
      'uri' => 'http://test-address.nope'
    }
  end

  before(:all) do
    ActiveJob::Base.queue_adapter = :test
  end

  before(:each) do
    @po = ProviderOrganization.new(npi: 10.times.map { rand(0..9) }.join, name: 'Test', id: 1)
    SyncOrganizationJob.perform_later(@po.id)
  end

  before do
    allow(DpcClient).to receive(:new).and_return(mock_dpc_client)
  end

  describe 'perform' do
    it 'creates an org in dpc-api if api_client returns no entry, then updates api org id' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [@po.id])
      allow(ProviderOrganization).to receive(:find).with(@po.id).and_return(@po)
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(@po.npi)
                                                                  .and_return(get_organization_zero_entries)
      expect(mock_dpc_client).to receive(:create_organization).with(have_attributes(name: @po.name, npi: @po.npi),
                                                                    fhir_endpoint:)
                                                              .and_return(create_organization_success_response)

      perform_enqueued_jobs
    end

    it 'updates provider_organization.npi if api_client returns an entry' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [@po.id])
      allow(ProviderOrganization).to receive(:find).with(@po.id).and_return(@po)
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(@po.npi)
                                                                  .and_return(get_organization_one_entry)
      expect(@po).to receive(:dpc_api_organization_id=).with(get_organization_one_entry.entry[0].resource.id)

      perform_enqueued_jobs
    end

    it 'raises an error if the provided unique ID is not found' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [@po.id])
      allow(ProviderOrganization).to receive(:find).with(@po.id).and_raise(ActiveRecord::RecordNotFound)
      expect do
        SyncOrganizationJob.perform_now(@po.id)
      end.to raise_error(SyncOrganizationJobError, "provider_organization #{@po.id} not found")
    end

    it 'raises an error if multiple orgs are found for the NPI in dpc_attribution' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [@po.id])
      allow(ProviderOrganization).to receive(:find).with(@po.id).and_return(@po)
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(@po.npi)
                                                                  .and_return(get_organization_two_entries)
      expect do
        SyncOrganizationJob.perform_now(@po.id)
      end.to raise_error(SyncOrganizationJobError, "multiple orgs found for NPI #{@po.npi} in dpc_attribution")
    end

    it 'raises an error if api_client.create_organization is unsuccessful' do
      assert_enqueued_with(job: SyncOrganizationJob, args: [@po.id])
      allow(ProviderOrganization).to receive(:find).with(@po.id).and_return(@po)
      expect(mock_dpc_client).to receive(:get_organization_by_npi).with(@po.npi)
                                                                  .and_return(get_organization_zero_entries)
      expect(mock_dpc_client).to receive(:create_organization).with(have_attributes(name: @po.name, npi: @po.npi),
                                                                    fhir_endpoint:)
                                                              .and_return(create_organization_unsuccessful_response)

      expect do
        SyncOrganizationJob.perform_now(@po.id)
      end.to raise_error(SyncOrganizationJobError,
                         "DpcClient.create_organization failed for provider_organization #{@po.id}")
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
