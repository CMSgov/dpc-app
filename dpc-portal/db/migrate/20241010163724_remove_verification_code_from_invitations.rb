class RemoveVerificationCodeFromInvitations < ActiveRecord::Migration[7.1]
  def change
    remove_column :invitations, :verification_code, :string
  end
end
