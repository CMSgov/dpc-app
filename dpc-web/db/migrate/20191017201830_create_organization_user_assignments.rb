class CreateOrganizationUserAssignments < ActiveRecord::Migration[5.2]
  def change
    remove_column :users, :organization_id

    create_table :organization_user_assignments do |t|
      t.integer :organization_id, null: false
      t.integer :user_id, null: false

      t.timestamps
    end

    add_index :organization_user_assignments, [:organization_id, :user_id], unique: true, name: 'index_org_user_assignments_on_organization_id_and_user_id'
  end
end
