class ChangeNullConstraint < ActiveRecord::Migration[5.2]
  def change
    change_column_null :internal_users, :email, true
  end
end
