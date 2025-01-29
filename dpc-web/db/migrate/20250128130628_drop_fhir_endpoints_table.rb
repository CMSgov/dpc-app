class DropFhirEndpointsTable < ActiveRecord::Migration[7.2]
  def change
    drop_table :fhir_endpoints, force: :cascade do |t|
      t.string "name", null: false
      t.integer "status", null: false
      t.string "uri", null: false
      t.integer "organization_id"
      t.integer "registered_organization_id"
      t.index ["registered_organization_id"], name: "index_fhir_endpoints_on_registered_organization_id"
    end
  end
end
