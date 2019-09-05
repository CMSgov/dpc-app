class AddUniquenessIndexToInternalUsers < ActiveRecord::Migration[5.2]
  def change
    add_index :internal_users, [:uid, :provider], unique: true

    remove_index :internal_users, :email
    remove_index :internal_users, :reset_password_token

    remove_column :internal_users, :reset_password_token
    remove_column :internal_users, :reset_password_sent_at
  end
end
