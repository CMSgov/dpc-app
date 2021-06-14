class DropInternalUsers < ActiveRecord::Migration[6.0]
  def change
    drop_table :internal_users
  end
end
