class RemoveRememberCreatedAtFromInternalUsers < ActiveRecord::Migration[5.2]
  def change
    remove_column :internal_users, :remember_created_at, :datetime
  end
end
