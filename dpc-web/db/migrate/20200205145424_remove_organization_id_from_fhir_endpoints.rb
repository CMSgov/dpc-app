class RemoveOrganizationIdFromFhirEndpoints < ActiveRecord::Migration[5.2]
  def change
    remove_column :fhir_endpoints, :organization_id, :integer
  end
end
