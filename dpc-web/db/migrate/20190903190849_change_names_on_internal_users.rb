class ChangeNamesOnInternalUsers < ActiveRecord::Migration[5.2]
  def change
   remove_column :internal_users, :first_name
   remove_column :internal_users, :last_name

   add_column :internal_users, :name, :string
   add_column :internal_users, :github_nickname, :string
  end
end
