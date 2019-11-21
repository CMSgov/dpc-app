class AddVendorToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :vendor, :string
  end
end
