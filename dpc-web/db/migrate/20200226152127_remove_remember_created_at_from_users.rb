class RemoveRememberCreatedAtFromUsers < ActiveRecord::Migration[5.2]
  def change
    remove_column :users, :remember_created_at, :datetime
  end
end
