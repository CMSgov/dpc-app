class AddOmniauthToInternalUsers < ActiveRecord::Migration[5.2]
  def change
    add_column :internal_users, :provider, :string
    add_column :internal_users, :uid, :string
  end
end
