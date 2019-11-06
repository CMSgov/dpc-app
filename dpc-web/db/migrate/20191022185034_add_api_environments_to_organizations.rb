class AddApiEnvironmentsToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :api_environments, :integer, array: true, default: []
  end
end
