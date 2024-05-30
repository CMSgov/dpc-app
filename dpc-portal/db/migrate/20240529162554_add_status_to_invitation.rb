class AddStatusToInvitation < ActiveRecord::Migration[7.1]
  def change
    add_column :invitations, :status, :integer
    remove_column :invitations, :cancelled_at, :datetime
  end
end
