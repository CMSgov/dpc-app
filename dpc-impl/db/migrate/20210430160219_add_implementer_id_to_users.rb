class AddImplementerIdToUsers < ActiveRecord::Migration[6.1]
  def change
    add_column :users, :implementer_id, :string
  end
end
