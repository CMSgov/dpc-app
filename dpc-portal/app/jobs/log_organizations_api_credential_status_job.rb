# frozen_string_literal: true

# A background job that determines the number of active Provider Organizations that
# have access to make API calls.
# This is determined by checking for tokens, public keys, and ip addresses for all
# organizations that are in the portal with completed Terms of Service agreement.
class LogOrganizationsApiCredentialStatusJob < ApplicationJob
  queue_as :portal

  def perform
    @start = Time.now
    organizations_credential_aggregate_status = {
      have_active_credentials: 0,
      have_incomplete_or_no_credentials: 0
    }
    ProviderOrganization.where.not(terms_of_service_accepted_by: nil).find_each do |organization|
      process_organization_credentials(organizations_credential_aggregate_status, organization.name,
                                       organization.dpc_api_organization_id)
    end
    Rails.logger.info(['Organizations API credential status', organizations_credential_aggregate_status])
  end

  def process_organization_credentials(organizations_credential_aggregate_status, organization_name,
                                       organization_id)
    credential_status = fetch_credential_status(organization_id)
    Rails.logger.info(['Credential status for organization',
                       { name: organization_name,
                         dpc_api_org_id: organization_id,
                         credential_status: }])
    update_organization_aggregate_hash(organizations_credential_aggregate_status, credential_status)
  rescue StandardError
    Rails.logger.error(['Failed to fetch api credential status for organization',
                        {  name: organization_name,
                           dpc_api_org_id: organization_id }])
  end

  def update_organization_aggregate_hash(aggregate_stats, credential_status)
    if credential_status[:num_tokens].zero? || credential_status[:num_keys].zero? || credential_status[:num_ips].zero?
      aggregate_stats[:have_incomplete_or_no_credentials] += 1
    else
      aggregate_stats[:have_active_credentials] += 1
    end
    aggregate_stats
  end

  def fetch_credential_status(organization_id)
    tokens = dpc_client.get_client_tokens(organization_id)
    current_datetime = DateTime.now
    active_tokens = tokens['entities'].select { |tok| tok['expiresAt'] > current_datetime }
    pub_keys = dpc_client.get_public_keys(organization_id)
    ip_addresses = dpc_client.get_ip_addresses(organization_id)

    {
      num_tokens: active_tokens.length,
      num_keys: pub_keys['count'],
      num_ips: ip_addresses['count']
    }
  end

  def dpc_client
    @dpc_client ||= DpcClient.new
  end
end
