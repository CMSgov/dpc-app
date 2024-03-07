class CreateAoOrgLinks < ActiveRecord::Migration[7.1]
  def change
    create_table :ao_org_links do |t|
      t.integer :user_id, null: false
      t.integer :provider_organization_id, null: false
      t.timestamps null: false
    end

    add_foreign_key :ao_org_links, :users, :user_id
    add_foreign_key :ao_org_links, :provider_organizations, :provider_organization_id
    add_index :ao_org_links, :user_id, unique: true
    add_index :ao_org_links, :provider_organization_id, unique: true
  end
end
