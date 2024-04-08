# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ProviderOrganization, type: :model do
  include ActiveJob::TestHelper

  describe :validations do
    it 'should pass if it has an npi' do
      expect(ProviderOrganization.new(npi: '1111111111')).to be_valid
    end

    it 'should fail without npi' do
      expect(ProviderOrganization.new).to_not be_valid
    end
  end

  describe 'after_create' do
    it 'should trigger SyncOrganizationJob' do
      ActiveJob::Base.queue_adapter = :test
      po = ProviderOrganization.new(npi: 10.times.map { rand(0..9) }.join, name: 'Test org')
      po.save
      assert_enqueued_with(job: SyncOrganizationJob, args: [po.id])
    end

    it 'should not trigger job if PO has dpc_api_organization_id' do
      ActiveJob::Base.queue_adapter = :test
      po = ProviderOrganization.new(
        npi: 10.times.map { rand(0..9) }.join,
        name: 'Test org',
        dpc_api_organization_id: 1)
      po.save
      assert_no_enqueued_jobs
    end
  end
end
