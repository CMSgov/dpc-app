class AddPasswordChangedAtToUsers < ActiveRecord::Migration[6.0]
  def change
    remove_column :users, :password_changed_at
    add_column :users, :password_changed_at, :datetime
  end
end
