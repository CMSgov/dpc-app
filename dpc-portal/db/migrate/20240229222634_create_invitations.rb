class CreateInvitations < ActiveRecord::Migration[7.0]
  def change
    create_table :invitations do |t|
      t.bigint :provider_organization_id, null: false
      t.bigint :invited_by_id, null: false
      t.integer :invitation_type, null: false
      t.string :invited_given_name
      t.string :invited_family_name
      t.string :invited_phone
      t.string :invited_email
      t.string :verification_code
      t.datetime :cancelled_at
      t.timestamps
    end

    add_foreign_key :invitations, :users, column: :invited_by_id
    add_foreign_key :invitations, :provider_organizations
  end
end
