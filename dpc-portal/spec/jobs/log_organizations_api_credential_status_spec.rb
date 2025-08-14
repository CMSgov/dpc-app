# frozen_string_literal: true

require 'rails_helper'

RSpec.describe LogOrganizationsApiCredentialStatusJob, type: :job do
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
  let(:mock_no_tokens_response) do
    {
      'entities' => [],
      'count' => 0,
      'created_at' => '2024-11-19T16:52:49.760+00:00'
    }
  end
  let(:mock_one_token_response) do
    {
      'entities' => [
        {
          'id' => 'f5b5559d-17b1-4951-a704-2f74b9b8587f',
          'tokenType' => 'MACAROON',
          'label' => 'Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.',
          'createdAt' => '2024-08-14T16:51:10.702+00:00',
          'expiresAt' => 1.day.from_now
        }
      ],
      'count' => 1,
      'created_at' => '2024-11-19T16:52:49.760+00:00'
    }
  end
  let(:mock_expired_token_response) do
    {
      'entities' => [
        {
          'id' => 'f5b5559d-17b1-4951-a704-2f74b9b8587f',
          'tokenType' => 'MACAROON',
          'label' => 'Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.',
          'createdAt' => '2024-08-14T16:51:10.702+00:00',
          'expiresAt' => 1.day.ago
        }
      ],
      'count' => 1,
      'created_at' => '2024-11-19T16:52:49.760+00:00'
    }
  end

  describe 'perform' do
    it 'organization has no api credentials' do
      provider_organization.save!

      expect(mock_dpc_client).to receive(:get_client_tokens).and_return(mock_no_tokens_response).once
      expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 0 }).once
      expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 0 }).once
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   { have_active_credentials: 0,
                                                     have_incomplete_or_no_credentials: 1 }])

      described_class.perform_now
    end
    it 'updates log with 1 organization that has all 3 credentials' do
      provider_organization.save!

      expect(mock_dpc_client).to receive(:get_client_tokens).and_return(mock_one_token_response).once
      expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 2 }).once
      expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 3 }).once
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   { have_active_credentials: 1,
                                                     have_incomplete_or_no_credentials: 0 }])

      described_class.perform_now
    end
    it 'updates log with 1 organization that has partial credentials' do
      provider_organization.save!

      expect(mock_dpc_client).to receive(:get_client_tokens).and_return(mock_one_token_response).once
      expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 2 }).once
      expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 0 }).once
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   { have_active_credentials: 0,
                                                     have_incomplete_or_no_credentials: 1 }])

      described_class.perform_now
    end
    it 'updates log with 1 organization that has expired credentials' do
      provider_organization.save!

      expect(mock_dpc_client).to receive(:get_client_tokens).and_return(mock_expired_token_response).once
      expect(mock_dpc_client).to receive(:get_public_keys).and_return({ 'count' => 2 }).once
      expect(mock_dpc_client).to receive(:get_ip_addresses).and_return({ 'count' => 0 }).once
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   { have_active_credentials: 0,
                                                     have_incomplete_or_no_credentials: 1 }])

      described_class.perform_now
    end
    it 'processes next organization successfully when an exception is raised' do
      second_organization = create(
        :provider_organization,
        name: 'Test2',
        dpc_api_organization_id: 'bar',
        terms_of_service_accepted_by: user,
        terms_of_service_accepted_at: 1.day.ago
      )
      second_organization.save!
      provider_organization.save!

      allow_any_instance_of(described_class).to receive(:fetch_credential_status).with('foo').and_return(
        num_tokens: 1, num_keys: 2, num_ips: 3
      )
      allow_any_instance_of(described_class).to receive(:fetch_credential_status).with('bar').and_raise(
        StandardError.new('something failed inside fetch_credential_status!')
      )

      allow(Rails.logger).to receive(:error)
      expect(Rails.logger).to receive(:error).with(['Failed to fetch api credential status for organization',
                                                    {  name: 'Test2',
                                                       dpc_api_org_id: 'bar' }])
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Organizations API credential status',
                                                   { have_active_credentials: 1,
                                                     have_incomplete_or_no_credentials: 0 }])
      described_class.perform_now
    end
  end
end
