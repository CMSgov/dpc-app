# frozen_string_literal: true

class LogOrganizationsAccessJob < ApplicationJob
  queue_as :portal

  def perform
    @start = Time.now
    ProviderOrganization.where.not(terms_of_service_accepted_by: nil).find_each do |organization|
      credential_status = fetch_credential_status?(organization)
      Rails.logger.info(['Credential status for organization',
                         { name: organization.name, id: organization.id,
                         has_prod_credentials: credential_status}])
    end
  end

  def fetch_credential_status?(organization)
      tokens = dpc_client.get_client_tokens(organization.id)
      pub_keys = dpc_client.get_public_keys(organization.id)
      ip_addresses = dpc_client.get_ip_addresses(organization.id)
      does_org_have_access = tokens["count"] >= 1 && pub_keys["count"] >= 1 && ip_addresses["count"] >= 1
      return does_org_have_access
  end

  def dpc_client
    @dpc_client ||= DpcClient.new
  end
end
