class CreateUserPurgeLogs < ActiveRecord::Migration[8.0]
  def change
    create_table :user_purge_logs do |t|
      t.bigint :user_id, null: false
      t.datetime :last_login_at, null: false
      t.datetime :purged_at, null: false

      t.timestamps
    end

    add_index :user_purge_logs, :user_id
  end
end
