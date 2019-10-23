class ChangeUsersNumProviders < ActiveRecord::Migration[5.2]
  def change
    rename_column :users, :num_providers, :requested_num_providers
  end
end
