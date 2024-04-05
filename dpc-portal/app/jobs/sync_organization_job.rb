# frozen_string_literal: true

# A background job that ensures that a provider_organization corresponds with the attribution_db
class SyncOrganizationJob < ApplicationJob
  queue_as :portal

  # rubocop:disable Metrics/AbcSize
  def perform(provider_organization_id)
    begin
      po = ProviderOrganization.find(provider_organization_id)
    rescue StandardError
      Rails.logger.error "provider_organization #{provider_organization_id} not found"
      raise SyncOrganizationJobError, "provider_organization #{provider_organization_id} not found"
    end

    api_response = api_client.get_organization_by_npi(po.npi)
    if api_response.entry.empty?
      create_dpc_api_org(po)
    elsif api_response.entry.length == 1
      org_id = api_response.entry[0].resource.id
      po.dpc_api_organization_id = org_id
      po.save
    else
      Rails.logger.error "multiple orgs found for NPI #{po.npi} in dpc_attribution"
      raise SyncOrganizationJobError, "multiple orgs found for NPI #{po.npi} in dpc_attribution"
    end
  end
  # rubocop:enable Metrics/AbcSize

  private

  def create_dpc_api_org(provider_organization)
    org = OrgObject.new(provider_organization.name, provider_organization.npi)
    fhir_endpoint = {
      'status' => 'test',
      'name' => "#{provider_organization.name} Endpoint",
      'uri' => 'http://test-address.nope'
    }
    create_org_response = api_client.create_organization(org, fhir_endpoint:)
    if create_org_response.response_successful?
      org_id = create_org_response.response_body['id']
      provider_organization.dpc_api_organization_id = org_id
      provider_organization.save
    else
      Rails.logger.error "DpcClient.create_organization failed for provider_organization #{provider_organization.id}"
      raise SyncOrganizationJobError,
            "DpcClient.create_organization failed for provider_organization #{provider_organization.id}"
    end
  end

  def api_client
    DpcClient.new
  end
end

class SyncOrganizationJobError < StandardError; end

# Org object required to pass to DpcClient#create_organization
class OrgObject
  # rubocop:disable Naming/VariableNumber
  attr_reader :npi, :name, :address_use, :address_type, :address_city, :address_state, :address_street,
              :address_street_2, :address_zip

  def initialize(name, npi)
    randy = Random.new
    @name = name
    @npi = npi
    @address_use = 'work'
    @address_type = 'both'
    @address_street = "#{randy.rand(1000)} Elm Street"
    @address_street_2 = "Suite #{randy.rand(100)}"
    @address_city = 'Akron'
    @address_state = 'OH'
    @address_zip = '22222'
  end
  # rubocop:enable Naming/VariableNumber
end
