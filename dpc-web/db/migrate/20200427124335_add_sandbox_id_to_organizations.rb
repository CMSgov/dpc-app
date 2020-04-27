class AddSandboxIdToOrganizations < ActiveRecord::Migration[5.2]
  def up
    add_column :organizations, :sandbox_id, :string

    Organization.find_each do |o|
      o.sandbox_id = Luhnacy.generate(15, :prefix => '808403')[-10..-1]
      o.save!
    end
  end

  def down
    remove_column :organizations, :sandbox_id
  end
end
