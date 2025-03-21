# frozen_string_literal: true

# A background job that checks whether a Provider Organization has an active Client Token,
# Public Key, and IP Address
class CheckConfigCompleteJob < ApplicationJob
  queue_as :portal

  def perform(org_id = nil)
    if org_id
      org = ProviderOrganization.find(org_id)
      org.check_config_complete
    else
      ProviderOrganization.where(config_complete: false).each(&:check_config_complete)
    end
  end
end
