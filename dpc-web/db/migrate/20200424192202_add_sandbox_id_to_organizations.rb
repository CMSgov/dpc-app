class AddSandboxIdToOrganizations < ActiveRecord::Migration[5.2]
  def up
    add_column :organizations, :sandbox_id, :string

    Organization.update_all sandbox_id: Luhnacy.generate(15, :prefix => '808403')[-10..-1]
  end

  def down
    remove_column :organizations, :sandbox_id
  end
end
