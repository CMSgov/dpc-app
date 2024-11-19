# frozen_string_literal: true

require 'rails_helper'

RSpec.describe LogOrganizationsAccessJob, type: :job do
  include ActiveJob::TestHelper

  let(:mock_dpc_client) { instance_double(DpcClient) }
  before do
    allow(DpcClient).to receive(:new).and_return(mock_dpc_client)
  end

  let(:user) do
    create(:user, provider: :openid_connect, uid: '12345',
                  verification_status: 'rejected', verification_reason: 'ao_med_sanctions')
  end
  let(:provider_organization) do
    create(
      :provider_organization,
      name: 'Test',
      dpc_api_organization_id: 'foo',
      terms_of_service_accepted_by: user,
      terms_of_service_accepted_at: 1.day.ago
    )
  end

  describe 'perform' do
    it 'organization has partial credentials' do
      provider_organization.save!

      expect(mock_dpc_client).to receive(:get_client_tokens).and_return({ 'count' => 0 }).once
      expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 0 }).once
      expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 0 }).once
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   { have_active_credentials: 0,
                                                     have_incomplete_or_no_credentials: 1,
                                                     have_no_credentials: 1 }])

      described_class.perform_now
    end
  end
  it 'updates log with 1 organization that has all 3 credentials' do
    provider_organization.save!

    expect(mock_dpc_client).to receive(:get_client_tokens).and_return({ 'count' => 1 }).once
    expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 2 }).once
    expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 3 }).once
    allow(Rails.logger).to receive(:info)
    expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                 { have_active_credentials: 1,
                                                   have_incomplete_or_no_credentials: 0,
                                                   have_no_credentials: 0 }])

    described_class.perform_now
  end
  it 'updates log with 1 organization that has partial credentials' do
    provider_organization.save!

    expect(mock_dpc_client).to receive(:get_client_tokens).and_return({ 'count' => 1 }).once
    expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 2 }).once
    expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 0 }).once
    allow(Rails.logger).to receive(:info)
    expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                 { have_active_credentials: 0,
                                                   have_incomplete_or_no_credentials: 1,
                                                   have_no_credentials: 0 }])

    described_class.perform_now
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
