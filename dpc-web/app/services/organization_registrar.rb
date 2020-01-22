# frozen_string_literal: true

class OrganizationRegistrar
  attr_reader :organization, :api_environments, :existing_envs

  def self.run(organization:, api_environments:)
    new(organization: organization, api_environments: api_environments).register_all
  end

  def initialize(organization:, api_environments:)
    @organization = organization
    @api_environments = api_environments
    @existing_envs = organization.registered_api_envs
  end

  def register_all
    remove_old_registered_organizations
    update_existing_registered_orgs
    register_new_organizations
  end

  def register_organization(api_env)
    create_sandbox_endpoint(api_env)

    api_client = APIClient.new(api_env).create_organization(organization)
    api_org = api_client.response_body

    create_registered_organization(api_env, api_org) if api_client.response_successful?
  end

  def update_existing_registered_orgs
    existing_registered_orgs.each do |registered_org|
      api_client = APIClient.new(registered_org.api_env)
      api_client.update_organization(registered_org)
      api_client.update_endpoint(registered_org)
    end
  end

  def existing_registered_orgs
    @existing_registered_orgs ||= organization.registered_organizations
  end

  private

  def remove_old_registered_organizations
    removed_envs = existing_envs - api_environments
    removed_reg_orgs = existing_registered_orgs.where(api_env: removed_envs)

    removed_reg_orgs.each do |registered_org|
      if APIClient.new(registered_org.api_env).delete_organization(registered_org)
        registered_org.destroy
      end
    end
  end

  def register_new_organizations
    added_envs = api_environments - existing_envs

    added_envs.each do |api_env|
      register_organization(api_env)
    end
  end

  def create_registered_organization(api_env, api_org)
    organization.registered_organizations.create(
      api_id: api_org['id'],
      api_env: api_env,
      api_endpoint_ref: api_org['endpoint'][0]['reference']
    )
  end

  def create_sandbox_endpoint(api_env)
    return unless api_env == 'sandbox' && organization.fhir_endpoints.empty?

    organization.fhir_endpoints.create(
      status: 'test', name: 'DPC Sandbox Test Endpoint',
      uri: 'https://dpc.cms.gov/test-endpoint'
    )
  end
end
