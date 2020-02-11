class AddVendorIdColumnToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :vendor_id, :string
  end
end
