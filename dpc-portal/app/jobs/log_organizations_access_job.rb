# frozen_string_literal: true

# A background job that determines the number of active Provider Organizations that
# have access to make API calls.
# This is determined by checking for tokens, public keys, and ip addresses for all
# organizations that are in the portal with completed Terms of Service agreement.
class LogOrganizationsAccessJob < ApplicationJob
  queue_as :portal

  def perform
    @start = Time.now
    organizations_credential_aggregate_status = {
      have_active_credentials: 0,
      have_incomplete_or_no_credentials: 0,
      have_no_credentials: 0
    }
    ProviderOrganization.where.not(terms_of_service_accepted_by: nil).find_each do |organization|
      credential_status = fetch_credential_status?(organization)
      Rails.logger.info(['Credential status for organization',
                         { name: organization.name, id: organization.id,
                           credential_status: }])
      credential_status_values_as_arr = [credential_status['num_tokens'], credential_status['num_keys'],
                                         credential_status['num_ips']]
      if credential_status_values_as_arr.all?
        organizations_credential_aggregate_status[:have_active_credentials] += 1
      elsif credential_status_values_as_arr.any?
        organizations_credential_aggregate_status[:have_incomplete_or_no_credentials] += 1
      else
        organizations_credential_aggregate_status[:have_incomplete_or_no_credentials] += 1
        organizations_credential_aggregate_status[:have_no_credentials] += 1
      end
    end
    Rails.logger.info(['Organizations API credential status', organizations_credential_aggregate_status])
  end

  def fetch_credential_status?(organization)
    tokens = dpc_client.get_client_tokens(organization.id)
    pub_keys = dpc_client.get_public_keys(organization.id)
    ip_addresses = dpc_client.get_ip_addresses(organization.id)

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
