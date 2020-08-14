class DropOldPasswords < ActiveRecord::Migration[6.0]
  def change
    drop_table :old_passwords
  end
end
