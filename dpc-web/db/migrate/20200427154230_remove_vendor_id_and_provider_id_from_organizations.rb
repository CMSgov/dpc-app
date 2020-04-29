class RemoveVendorIdAndProviderIdFromOrganizations < ActiveRecord::Migration[5.2]
  def change
    remove_column :organizations, :vendor_id
    remove_column :organizations, :provider_id
  end
end
