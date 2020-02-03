class DropIndexFromFhirEndpoints < ActiveRecord::Migration[5.2]
  def change
    change_column_null :fhir_endpoints, :organization_id, true
    remove_index :fhir_endpoints, :organization_id
  end
end
