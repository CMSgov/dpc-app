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
      have_incomplete_or_no_credentials: 0,
      have_no_credentials: 0
    }
    ProviderOrganization.where.not(terms_of_service_accepted_by: nil).find_each do |organization|
      credential_status = fetch_credential_status(organization.dpc_api_organization_id)
      Rails.logger.info(['Credential status for organization',
                         { name: organization.name,
                           dpc_api_org_id: organization.dpc_api_organization_id,
                           credential_status: }])
      credential_status_values_as_arr = [credential_status[:num_tokens], credential_status[:num_keys],
                                         credential_status[:num_ips]]
      update_organization_aggregate_hash(organizations_credential_aggregate_status, credential_status_values_as_arr)
    end
    Rails.logger.info(['Organizations API credential status', organizations_credential_aggregate_status])
  end

  def update_organization_aggregate_hash(aggregate_stats, organization_credentials_as_arr)
    zero_count = organization_credentials_as_arr.count(0)
    if zero_count.zero?
      aggregate_stats[:have_active_credentials] += 1
    elsif zero_count == 1
      aggregate_stats[:have_incomplete_or_no_credentials] += 1
    else
      aggregate_stats[:have_incomplete_or_no_credentials] += 1
      aggregate_stats[:have_no_credentials] += 1
    end
    aggregate_stats
  end

  def fetch_credential_status(organization_id)
    tokens = dpc_client.get_client_tokens(organization_id)
    pub_keys = dpc_client.get_public_keys(organization_id)
    ip_addresses = dpc_client.get_ip_addresses(organization_id)

    {
      num_tokens: tokens['count'],
      num_keys: pub_keys['count'],
      num_ips: ip_addresses['count']
    }
  end

  def dpc_client
    @dpc_client ||= DpcClient.new
  end
end
