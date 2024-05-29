class AddStatusToInvitation < ActiveRecord::Migration[7.1]
  def change
    add_column :invitations, :status, :integer
  end
end
