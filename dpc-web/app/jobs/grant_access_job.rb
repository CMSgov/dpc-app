# frozen_string_literal: true

class GrantAccessJob < ApplicationJob
  def perform(user_id)
    user = User.find user_id

    organization = find_or_create_org(user)

    unless organization.registered_organization.present?
      registered_organization = organization.build_registered_organization
      registered_organization.build_default_fhir_endpoint
      registered_organization.save!
    end

    # Add the user after creating the RegisteredOrganization to make sure email is sent
    organization.users << user
    logger.info 'GrantAccessJob success'
    organization
  rescue ActiveRecord::RecordNotFound => e
    logger.error "GrantAccessJob failure: #{e.message}"
  rescue ActiveRecord::RecordInvalid, ActiveRecord::RecordNotSaved => e
    record = e.record
    logger.error "GrantAccessJob failure creating #{record.class.name}: #{record.errors.full_messages.join(' | ')}"
  end

  def find_or_create_org(user)
    orgs = Organization.where('name ~* ?', user.requested_organization)
    return orgs.first if orgs.one?

    Organization.find_or_create_by!(name: user.requested_organization) do |org|
      org.organization_type = user.requested_organization_type
      org.num_providers = user.requested_num_providers
      org.assign_id
      org.build_address street: user.address_1,
                        'street_2' => user.address_2,
                        city: user.city,
                        state: user.state,
                        zip: user.zip
    end
  end
end
