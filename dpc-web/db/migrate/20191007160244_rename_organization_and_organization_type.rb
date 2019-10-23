class RenameOrganizationAndOrganizationType < ActiveRecord::Migration[5.2]
  def change
    rename_column :users, :organization, :requested_organization
    rename_column :users, :organization_type, :requested_organization_type
  end
end
