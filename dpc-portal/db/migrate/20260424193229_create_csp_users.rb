class CreateCspUsers < ActiveRecord::Migration[8.0]
  def change
    create_table :csp_users do |t|
      t.references :user, null: false, foreign_key: true
      t.references :csp, null: false, foreign_key: true
      t.uuid :uuid

      t.timestamps
    end

    add_index :csp_users, [:user_id, :csp_id], unique: true

    # Populate existing users with login_dot_gov
    User.find_each do |user|
      CspUser.create!(user: user, uuid: user.uid, csp: Csp.find_by(name: :login_dot_gov))
    end
  end
end
