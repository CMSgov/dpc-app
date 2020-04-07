class AddProvidorIdToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :provider_id, :string
  end
end
