class AddEnabledToRegisteredOrganizations < ActiveRecord::Migration[6.0]
  def change
    add_column :registered_organizations, :enabled, :boolean, :default => true

    RegisteredOrganization.where(enabled: nil).update_all(enabled: true)
  end
end
