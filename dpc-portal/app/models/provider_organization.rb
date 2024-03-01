# frozen_string_literal: true

# Link class to dpc-api Organization
class ProviderOrganization < ApplicationRecord
  validates :npi, presence: true

  def path_id
    dpc_api_organization_id
  end
end
