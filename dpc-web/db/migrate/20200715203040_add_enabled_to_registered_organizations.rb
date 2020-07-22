class AddEnabledToRegisteredOrganizations < ActiveRecord::Migration[6.0]
  def change
    add_column :registered_organizations, :enabled, :boolean, :default => true, null: false
  end
end
