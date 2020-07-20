class AddEnabledToRegisteredOrganizations < ActiveRecord::Migration[6.0]
  def up
    add_column :registered_organizations, :enabled, :boolean, null: false

    RegisteredOrganization.where(enabled: nil).update_all(enabled: true)
  end

  def down
    remove_column :registered_organizations, :enabled
  end
end
