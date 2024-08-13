# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ProviderOrganization, type: :model do
  include ActiveJob::TestHelper
  let(:mock_ctm) { instance_double(ClientTokenManager) }

  before do
    allow(ClientTokenManager).to receive(:new).and_return(mock_ctm)
  end

  describe :validations do
    let(:provider_organization) { create(:provider_organization) }

    it 'should pass if it has an npi' do
      expect(ProviderOrganization.new(npi: '1111111111')).to be_valid
    end

    it 'should fail without npi' do
      expect(ProviderOrganization.new).to_not be_valid
    end

    it 'fails on invalid verification_reason' do
      expect do
        provider_organization.verification_reason = :fake_reason
      end.to raise_error(ArgumentError)
    end

    it 'allows good verification_reason' do
      expect do
        provider_organization.verification_reason = :org_med_sanction_waived
        provider_organization.save
      end.not_to raise_error
    end

    it 'allows blank verification_reason' do
      expect do
        provider_organization.verification_reason = ''
        provider_organization.save
      end.not_to raise_error
    end

    it 'allows nil verification_reason' do
      expect do
        provider_organization.verification_reason = nil
        provider_organization.save
      end.not_to raise_error
    end

    it 'fails on invalid verification_status' do
      expect do
        provider_organization.verification_status = :fake_status
      end.to raise_error(ArgumentError)
    end

    it 'allows good verification_status' do
      expect do
        provider_organization.verification_status = :approved
        provider_organization.save
      end.not_to raise_error
    end

    it 'allows nil verification_status' do
      expect do
        provider_organization.verification_status = nil
        provider_organization.save
      end.not_to raise_error
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
        dpc_api_organization_id: 1
      )
      po.save
      assert_no_enqueued_jobs
    end
  end

  describe 'disable_rejected' do
    it 'should delete client tokens' do
      po = create(:provider_organization, dpc_api_organization_id: SecureRandom.uuid, verification_status: :approved)
      po.save
      tokens = [{ 'id' => 'abcdef' }, { 'id' => 'ftguiol' }]
      allow(mock_ctm).to receive(:client_tokens).and_return(tokens)
      expect(mock_ctm).to receive(:delete_client_token).with(tokens[0])
      expect(mock_ctm).to receive(:delete_client_token).with(tokens[1])
      po.verification_status = :rejected
      expect do
        po.save
      end.to change { CredentialAuditLog.count }.by 2
      tokens.each do |token|
        expect(CredentialAuditLog.where(dpc_api_credential_id: token['id'], action: 'remove')).to exist
      end
    end
  end
end
