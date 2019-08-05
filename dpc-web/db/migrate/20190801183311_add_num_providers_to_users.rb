class AddNumProvidersToUsers < ActiveRecord::Migration[5.2]
  def change
    add_column :users, :num_providers, :integer, default: 0
  end
end
