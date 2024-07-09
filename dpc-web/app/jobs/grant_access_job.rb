# frozen_string_literal: true

class GrantAccessJob < ApplicationJob
  def perform(user_id)
    # Get user
    # Check if registered
    # Check if organization exists (by name)
    # Make org
    # Make registered org
    user = User.find user_id

    organization = Organization.find_or_create_by(name: user.requested_organization) do |org|
      org.organization_type = user.requested_organization_type
      org.num_providers = user.requested_num_providers
      org.assign_id
      org.build_address street: user.address_1,
                        'street_2' => user.address_2,
                        city: user.city,
                        state: user.state,
                        zip: user.zip
    end

    unless organization.registered_organization.present?
      registered_organization = organization.build_registered_organization
      registered_organization.build_default_fhir_endpoint
      registered_organization.save
    end
    # Add the user at the end to make sure email is sent
    organization.users << user
    organization
  end
end
