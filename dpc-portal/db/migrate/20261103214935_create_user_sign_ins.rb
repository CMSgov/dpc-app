class CreateUserSignIns < ActiveRecord::Migration[8.0]
  def change
    create_table :user_sign_ins do |t|
      t.bigint :user_id, null: false
      t.bigint :csp_id, null: false
      t.datetime :last_login_at, null: false

      t.timestamps
    end

    add_index :user_sign_ins, %i[user_id csp_id], unique: true
    add_foreign_key :user_sign_ins, :users
    add_foreign_key :user_sign_ins, :csps
  end
end
