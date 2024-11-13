# frozen_string_literal: true

class LogOrganizationsAccessJob < ApplicationJob
  queue_as :portal

  def perform
    @start = Time.now
    ProviderOrganization.where.not(terms_of_service_accepted_by: nil).find_each do |organization|
      fetch_credential_status(organization)
    end
  end

  def fetch_credential_status(organization)
      tokens = dpc_client.get_client_tokens(organization.id)
      pub_keys = dpc_client.get_public_keys(organization.id)
      return {
        "tokens": tokens,
        "public_keys": pub_keys,
      }
  end

  def dpc_client
    @dpc_client ||= DpcClient.new
  end
end
