class RemoveVerificationCodeFromInvitations < ActiveRecord::Migration[7.1]
  def change
    remove_column :invitations, :verification_code, :string
    remove_column :invitations, :failed_attempts, :integer
  end
end
