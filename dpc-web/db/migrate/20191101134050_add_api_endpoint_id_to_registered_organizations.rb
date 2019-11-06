class AddApiEndpointIdToRegisteredOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :registered_organizations, :api_endpoint_ref, :string
  end
end
