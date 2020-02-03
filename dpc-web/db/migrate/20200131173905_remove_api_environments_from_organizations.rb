class RemoveApiEnvironmentsFromOrganizations < ActiveRecord::Migration[5.2]
  def change
    remove_column :organizations, :api_environments
  end
end
