# frozen_string_literal: true

class OrganizationRegistrar
  attr_reader :organization, :api_environments, :existing_envs

  def initialize(organization:, api_environments:)
    @organization = organization
    @api_environments = api_environments
    @existing_envs = organization.api_environments
  end

  def register_all
    return true if no_change?

    remove_old_registered_organizations
    register_new_organizations
  end

  private

  def no_change?
    existing_envs.sort == api_environments.sort
  end

  def remove_old_registered_organizations
    removed_envs = existing_envs - api_environments
    removed_reg_orgs = organization.registered_organizations.where(api_env: removed_envs)

    removed_reg_orgs.each do |registered_org|
      if APIClient.new(registered_org.api_env).delete_organization(registered_org)
        registered_org.destroy
      end
    end
  end

  def register_new_organizations
    added_envs = api_environments - existing_envs

    added_envs.each do |api_env|
      if (api_org = APIClient.new(api_env).create_organization(organization))
        organization.registered_organizations.build api_id: api_org['id'], api_env: api_env
      end
    end
    organization.save
    # TODO save profile endpoint ID?
  end
end
