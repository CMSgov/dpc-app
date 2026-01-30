class CreateUserCredentials < ActiveRecord::Migration[8.0]
  def up
    create_table :user_credentials do |t|
      t.string :provider
      t.string :uid
      t.integer :user_id

      t.timestamps
    end
    User.all.each do |user|
      UserCredential.create!(user:, uid: user.uid, provider: user.provider)
    end
    add_index(:user_credentials, %i[provider uid])
  end
  def down
    drop_table :user_credentials
  end
end
