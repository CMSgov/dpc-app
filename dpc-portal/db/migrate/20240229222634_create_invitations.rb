class CreateInvitations < ActiveRecord::Migration[7.0]
  def change
    create_table :invitations do |t|
      t.bigint :provider_organization_id
      t.bigint :invited_by_id
      t.string :invitation_type
      t.string :invited_given_name
      t.string :invited_family_name
      t.string :invited_phone
      t.string :invited_email
      t.string :verification_code
      t.datetime :cancelled_at
      t.timestamps
    end
  end
end
