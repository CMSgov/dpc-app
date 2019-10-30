class CreateRegisteredOrganizations < ActiveRecord::Migration[5.2]
  def change
    create_table :registered_organizations do |t|
      t.integer :organization_id, null: false
      t.string :api_id, null: false
      t.integer :api_env, null: false

      t.timestamps
    end

    add_index :registered_organizations, :organization_id

    remove_column :organizations, :api_environments
  end
end
