# frozen_string_literal: true

# Link class to dpc-api Organization
class ProviderOrganization < ApplicationRecord
  validates :npi, presence: true

  belongs_to :terms_of_service_accepted_by, class_name: 'User', required: false

  def path_id
    dpc_api_organization_id
  end
end
