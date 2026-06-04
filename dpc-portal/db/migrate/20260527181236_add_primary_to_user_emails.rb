class AddPrimaryToUserEmails < ActiveRecord::Migration[8.0]
  def change
    add_column :user_emails, :primary, :boolean, default: false, null: false

    # Make sure each csp_user can only have one primary email.
    add_index :user_emails, :csp_user_id, 
              unique: true, 
              where: '"primary" = true', 
              name: 'index_unique_primary_email_per_csp_user'
  end
end
