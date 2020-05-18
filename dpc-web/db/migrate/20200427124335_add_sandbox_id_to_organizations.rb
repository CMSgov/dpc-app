class AddSandboxIdToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :sandbox_id, :string
  end
end
