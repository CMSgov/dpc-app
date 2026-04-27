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
  end
end
