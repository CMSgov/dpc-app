class UpdateInvitationsMakeInvitedByIdNullable < ActiveRecord::Migration[7.1]
  def change
    change_column_null :invitations, :invited_by_id, true
  end
end
