class RemoveSandboxIdFromOrganizations < ActiveRecord::Migration[6.0]
  def change
    remove_column :organizations, :sandbox_id, :string
  end
end
