class CreateCdOrgLinks < ActiveRecord::Migration[7.1]
  def change
    create_table :cd_org_links do |t|
      t.integer :user_id, null: false
      t.integer :provider_organization_id, null: false
      t.integer :invitation_id, null: false
      t.datetime :disabled_at
      t.timestamps null: false
    end

    add_foreign_key :cd_org_links, :users
    add_foreign_key :cd_org_links, :provider_organizations
    add_foreign_key :cd_org_links, :invitations
  end
end
