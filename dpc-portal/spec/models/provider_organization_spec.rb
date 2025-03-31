# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ProviderOrganization, type: :model do
  include ActiveJob::TestHelper

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
    let(:mock_ctm) { instance_double(ClientTokenManager) }

    before do
      allow(ClientTokenManager).to receive(:new).and_return(mock_ctm)
    end

    let(:org) do
      create(:provider_organization, dpc_api_organization_id: SecureRandom.uuid, verification_status: :approved)
    end
    it 'should delete client tokens' do
      tokens = [{ 'id' => 'abcdef' }, { 'id' => 'ftguiol' }]
      allow(mock_ctm).to receive(:client_tokens).and_return(tokens)
      tokens.each { |token| expect(mock_ctm).to receive(:delete_client_token).with(token) }
      expect do
        org.update(verification_status: :rejected)
      end.to change { CredentialAuditLog.count }.by 2
      tokens.each do |token|
        expect(CredentialAuditLog.where(dpc_api_credential_id: token['id'], action: 'remove')).to exist
      end
    end
    it 'should log API disabled' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info)
        .with(['Org API disabled',
               { actionType: LoggingConstants::ActionType::ApiBlocked,
                 providerOrganization: org.id }])

      tokens = [{ 'id' => 'abcdef' }, { 'id' => 'ftguiol' }]
      allow(mock_ctm).to receive(:client_tokens).and_return(tokens)
      tokens.each { |token| expect(mock_ctm).to receive(:delete_client_token).with(token) }
      org.update(verification_status: :rejected)
    end

    it 'should log API disabled with comments if available' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info)
        .with(['Org API disabled',
               { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                 actionType: LoggingConstants::ActionType::ApiBlocked,
                 providerOrganization: org.id }])

      tokens = [{ 'id' => 'abcdef' }, { 'id' => 'ftguiol' }]
      allow(mock_ctm).to receive(:client_tokens).and_return(tokens)
      tokens.each { |token| expect(mock_ctm).to receive(:delete_client_token).with(token) }
      org.update(verification_status: :rejected, audit_comment: LoggingConstants::ActionContext::BatchVerificationCheck)
    end

    it 'should not log API disabled if no tokens' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to_not receive(:info)
        .with(['Org API disabled',
               { actionType: LoggingConstants::ActionType::ApiBlocked,
                 providerOrganization: org.id }])

      allow(mock_ctm).to receive(:client_tokens).and_return([])
      org.update(verification_status: :rejected)
    end
  end

  describe :audits do
    let(:org) { create(:provider_organization) }
    it 'should not audit name' do
      org.update(name: 'Botox Bonanza')
      expect(org.audits.count).to eq 0
    end
    it 'should not audit npi' do
      org.update(npi: 'another-npi')
      expect(org.audits.count).to eq 0
    end
    it 'should audit verification_status' do
      org.update(verification_status: :rejected)
      expect(org.audits.count).to eq 1
    end
    it 'should audit verification_reason' do
      org.update(verification_reason: :org_med_sanctions)
      expect(org.audits.count).to eq 1
    end
    it 'should audit verification_status and _reason together' do
      org.update(verification_status: :rejected, verification_reason: :org_med_sanctions)
      expect(org.audits.count).to eq 1
    end
  end

  describe :ao do
    it 'should return name if AO exists' do
      org = create(:provider_organization)
      user = create(:user, given_name: 'John', family_name: 'Doe')
      create(:ao_org_link, user:, provider_organization: org, verification_status: true)
      expect(org.ao).to eq('John Doe')
    end

    it 'should return blank if AO does not exist' do
      noAoOrg = create(:provider_organization)
      expect(noAoOrg.ao).to eq('')
    end
  end

  describe :check_config_complete do
    let(:org) { create(:provider_organization, dpc_api_organization_id: 'some-guid') }
    it 'should mark org complete if has all credentials' do
      expect(org.config_complete).to be false
      set_credentials :client_token, :public_key, :ip_address
      org.check_config_complete
      expect(org.config_complete).to be true
    end
    it 'should not mark org complete if no ip addresses' do
      expect(org.config_complete).to be false
      set_credentials :client_token, :public_key
      org.check_config_complete
      expect(org.config_complete).to be false
    end
    it 'should not mark org complete if no public keys' do
      expect(org.config_complete).to be false
      set_credentials :client_token, :ip_address
      org.check_config_complete
      expect(org.config_complete).to be false
    end
    it 'should not mark org complete if no client tokens' do
      expect(org.config_complete).to be false
      set_credentials :public_key, :ip_address
      org.check_config_complete
      expect(org.config_complete).to be false
    end
    it 'should update if no longer complete' do
      org.update(config_complete: true)
      expect(org.config_complete).to be true
      set_credentials :public_key, :ip_address
      org.check_config_complete
      expect(org.config_complete).to be false
    end
  end

  # rubocop:disable Metrics/AbcSize
  def set_credentials(*args)
    tokens = []
    tokens << { 'token' => 'exampleToken' } if args.include?(:client_token)
    mock_ctm = instance_double(ClientTokenManager)
    allow(ClientTokenManager).to receive(:new).and_return(mock_ctm)
    allow(mock_ctm).to receive(:client_tokens).and_return(tokens)

    keys = []
    keys << { 'id' => 'key-guid' } if args.include?(:public_key)
    mock_pkm = instance_double(PublicKeyManager)
    allow(PublicKeyManager).to receive(:new).and_return(mock_pkm)
    allow(mock_pkm).to receive(:public_keys).and_return(keys)

    addresses = []
    addresses << { 'id' => 'address-guid' } if args.include?(:ip_address)
    mock_ipm = instance_double(IpAddressManager)
    allow(IpAddressManager).to receive(:new).and_return(mock_ipm)
    allow(mock_ipm).to receive(:ip_addresses).and_return(addresses)
  end
  # rubocop:enable Metrics/AbcSize
end
