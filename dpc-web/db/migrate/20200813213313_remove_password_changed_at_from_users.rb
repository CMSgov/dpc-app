class RemovePasswordChangedAtFromUsers < ActiveRecord::Migration[6.0]
  def change
    remove_column :users, :password_changed_at
  end
end
