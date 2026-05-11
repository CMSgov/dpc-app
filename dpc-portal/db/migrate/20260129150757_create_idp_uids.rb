class CreateIdpUids < ActiveRecord::Migration[8.0]
  def up
    create_table :idp_uids do |t|
      t.string :provider
      t.string :uid
      t.integer :user_id

      t.timestamps
    end
    User.all.each do |user|
      IdpUid.create!(user:, uid: user.uid, provider: user.provider)
    end
    add_index(:idp_uids, %i[provider uid])
  end
  def down
    drop_table :idp_uids
  end
end
