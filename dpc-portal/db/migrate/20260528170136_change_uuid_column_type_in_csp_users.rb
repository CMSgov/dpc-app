class ChangeUuidColumnTypeInCspUsers < ActiveRecord::Migration[8.0]
  def change
    change_column(:csp_users, :uuid, :string)
  end
end
