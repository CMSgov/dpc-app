class AddSandboxIdToOrganizations < ActiveRecord::Migration[5.2]
  def change
    add_column :organizations, :sandbox_id, :string

    Organization.where(sandbox_id: nil).update_all(sandbox_id: Luhnacy.generate(15, :prefix => '808403')[-10..-1])
  end
end
