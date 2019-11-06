class AddApiFieldsToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :npi, :string

    create_table :fhir_endpoints do |t|
      t.string :name, null: false
      t.integer :status, null: false
      t.integer :connection_type, null: false
      t.string :uri, null: false

      t.integer :organization_id, null: false
    end

    add_index :fhir_endpoints, :organization_id
  end
end
