class CreateOrganizations < ActiveRecord::Migration[5.2]
  def change
    create_table :organizations do |t|
      t.string :name, null: false
      t.integer :organization_type, null: false
      t.integer :num_providers, default: 0

      t.timestamps
    end
  end
end
