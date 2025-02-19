class AddVerificationToUsers < ActiveRecord::Migration[7.1]
  def change
    add_column :users, :pac_id, :string
    add_column :users, :verification_status, :integer
    add_column :users, :verification_reason, :integer
    add_column :users, :last_checked_at, :datetime
  end
end
