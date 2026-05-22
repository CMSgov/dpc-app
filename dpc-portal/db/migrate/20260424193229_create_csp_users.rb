class CreateCspUsers < ActiveRecord::Migration[8.0]
  def up
    create_table :csp_users do |t|
      t.references :user, null: false, foreign_key: true
      t.references :csp, null: false, foreign_key: true
      t.uuid :uuid

      t.timestamps
    end

    add_index :csp_users, [:user_id, :csp_id], unique: true

    # Populate existing users with login_dot_gov
    Csp.find_by(name: :login_dot_gov)&.tap do |csp|
      User.find_each do |user|
        # Every user initially created with "openid_connect" should be updated to "login_dot_gov"
        if user.provider == "openid_connect"
          user.update_columns(provider: "login_dot_gov")
        end

        CspUser.find_or_create_by!(user: user, csp: csp) do |csp_user|
          csp_user.uuid = user.uid
        end
      end
    end
  end

  def down
    User.where(provider: "login_dot_gov").update_all(provider: "openid_connect")
    drop_table :csp_users
  end
end
