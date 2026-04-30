class CreateUserEmails < ActiveRecord::Migration[8.0]
  def change
    create_table :user_emails do |t|
      t.references :csp_user, null: false, foreign_key: true
      t.string :email
      t.boolean :active
      t.datetime :deactivated_at
      t.datetime :reactivated_at

      t.timestamps
    end

    add_index :user_emails, [:csp_user_id, :email], unique: true
  end
end
