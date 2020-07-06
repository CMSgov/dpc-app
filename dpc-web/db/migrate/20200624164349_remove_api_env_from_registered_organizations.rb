class RemoveApiEnvFromRegisteredOrganizations < ActiveRecord::Migration[6.0]
  def change
    remove_column :registered_organizations, :api_env
  end
end
