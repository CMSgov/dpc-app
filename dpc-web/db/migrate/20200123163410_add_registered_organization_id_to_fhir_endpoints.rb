class AddRegisteredOrganizationIdToFhirEndpoints < ActiveRecord::Migration[5.2]
  def change
    add_column :fhir_endpoints, :registered_organization_id, :integer

    add_index :fhir_endpoints, :registered_organization_id
  end
end
