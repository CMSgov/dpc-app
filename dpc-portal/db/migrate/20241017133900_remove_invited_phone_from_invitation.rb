class RemoveInvitedPhoneFromInvitation < ActiveRecord::Migration[7.1]
  def change
    remove_column :invitations, :invited_phone, :string
  end
end
