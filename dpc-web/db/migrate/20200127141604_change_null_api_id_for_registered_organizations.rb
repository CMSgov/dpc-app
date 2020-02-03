class ChangeNullApiIdForRegisteredOrganizations < ActiveRecord::Migration[5.2]
  def change
    change_column_null :registered_organizations, :api_id, true
  end
end
