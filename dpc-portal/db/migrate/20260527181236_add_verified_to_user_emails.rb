class AddVerifiedToUserEmails < ActiveRecord::Migration[8.0]
  def change
    add_column :user_emails, :verified, :boolean, default: false, null: false

    # Make sure each csp_user can only have one verified email.
    add_index :user_emails, :csp_user_id, 
              unique: true, 
              where: '"verified" = true', 
              name: 'index_unique_verified_email_per_csp_user'
  end
end
