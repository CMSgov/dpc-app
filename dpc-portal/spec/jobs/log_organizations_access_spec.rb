# frozen_string_literal: true

require 'rails_helper'

RSpec.describe LogOrganizationsAccessJob, type: :job do
  include ActiveJob::TestHelper

  let(:mock_dpc_client) { instance_double(DpcClient) }

  let(:provider_organization) do
    create(
      :provider_organization,
      name: 'Test',
      dpc_api_organization_id: 'foo'
    )
  end

  # before(:each) do
  #   SyncOrganizationJob.perform_later(provider_organization.id)
  # end

  before do
    allow(DpcClient).to receive(:new).and_return(mock_dpc_client)
  end

  describe 'perform' do
    it 'creates an org in dpc-api if api_client returns no entry, then updates api org id' do
      allow(Rails.logger).to receive(:info)
      # expect(Rails.logger).to have_received(:info).with('test update')
      expect(Rails.logger).to receive(:info).with('random message')
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   {have_active_credentials: 0,
                                                    have_incomplete_or_no_credentials: 1,
                                                    have_no_credentials: 1 }])

      described_class.perform_now
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
