class CreateAoOrgLinks < ActiveRecord::Migration[7.1]
  def change
    create_table :ao_org_links do |t|
      t.integer :user_id, null: false
      t.integer :provider_organization_id, null: false
      t.timestamps null: false
      # ensure only one assignment per user-org
      t.index [:user_id, :provider_organization_id], unique: true
    end

    add_foreign_key :ao_org_links, :users
    add_foreign_key :ao_org_links, :provider_organizations
  end
end
