class FhirEndpointDataMigration < ActiveRecord::Migration[5.2]
  def up
    FhirEndpoint.all.each do |fhir_endpoint|
      # Skip if this fhir endpoint has a registered organization (meaning it was created after the refactor)
      next if fhir_endpoint.registered_organization.present?

      organization = Organization.find_by id: fhir_endpoint.organization_id

      # If organization, carry on. If not, then this is an orphaned fhir_endpoint record
      # that should be destroyed.
      if organization
        # There shouldn't be more than one regsitered organization, but on the off chance there is,
        # let's use the most recent one.
        reg_org = organization.registered_organizations.last

        # If there is a registered organization, it must be sandbox, so let's use that one. If there isn't
        # a registered organization, then the org was never created in the API, so the endpoint shouldn't exist.
        # So, let's destroy it.
        if reg_org
          # Remove organization_id value so that the end result of this migration is as close to
          # the desired end state as possible (no organization_id column).
          # This lets us validate in user testing that it is not needed at all.
          fhir_endpoint.update(registered_organization_id: reg_org.id, organization_id: nil)
        else
          fhir_endpoint.destroy
        end
      else
        fhir_endpoint.destroy
      end
    end
  end

  # To undo this migration:
  def down
    FhirEndpoint.all.each do |fhir_endpoint|
      reg_org = fhir_endpoint.registered_organization

      # If registered_organization exists, carry on. If not, then this is an orphaned fhir_endpoint record
      # that should be destroyed.
      if reg_org
        # Can't reset registered_organization_id to nil because then record is invalid.
        fhir_endpoint.update(organization_id: reg_org.organization_id)
      else
        fhir_endpoint.destroy
      end
    end
  end
end
