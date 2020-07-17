class RemoveApiIdAndReferenceFromOrganizations < ActiveRecord::Migration[6.0]
  def change
    remove_column :organizations, :api_id
    remove_column :organizations, :api_reference
  end
end
