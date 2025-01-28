class DropFhirEndpointsTable < ActiveRecord::Migration[7.2]
  def change
    drop_table :fhir_endpoints
  end
end
