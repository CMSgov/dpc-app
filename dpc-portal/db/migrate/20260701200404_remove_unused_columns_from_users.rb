class RemoveUnusedColumnsFromUsers < ActiveRecord::Migration[8.0]
  def up
    # Backfill user_emails from existing user.email before dropping the column
    User.find_each do |user|
      next if user.email.blank?

      csp_user = CspUser.find_by(user: user)
      next if csp_user.nil?

      # Create the primary email record if it doesn't already exist
      UserEmail.find_or_create_by!(csp_user: csp_user, email: user.email) do |user_email|
        user_email.active    = true
        user_email.primary   = true
      end
    end

    remove_column :users, :encrypted_password, :string
    remove_column :users, :reset_password_token, :string
    remove_column :users, :reset_password_sent_at, :datetime
    remove_column :users, :uid, :string
    remove_column :users, :email, :string
    remove_column :users, :provider, :string
  end

  def down
    add_column :users, :encrypted_password, :string
    add_column :users, :reset_password_token, :string
    add_column :users, :reset_password_sent_at, :datetime
    add_column :users, :uid, :string
    add_column :users, :email, :string
    add_column :users, :provider, :string

    # Restore emails from user_emails back to users.email on rollback
    UserEmail.where(primary: true).find_each do |user_email|
      user = user_email.csp_user&.user
      next if user.nil?

      user.update_columns(
        email: user_email.email,
        uid: user_email.csp_user.uuid,
        provider: user_email.csp_user.csp&.name
      )
    end
  end
end
