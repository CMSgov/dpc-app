class AddFailedAttemptsToInvitations < ActiveRecord::Migration[7.1]
  def change
    add_column :invitations, :failed_attempts, :integer, default: 0, null: false
  end
end
