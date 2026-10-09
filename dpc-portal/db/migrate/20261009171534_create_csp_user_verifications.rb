class CreateCspUserVerifications < ActiveRecord::Migration[8.0]
  def up
    create_table :csp_user_verifications do |t|
      t.references :csp_user, null: false, foreign_key: { to_table: :csp_users }, index: true

      t.string :csp_verification_id, null: false
      t.string :status, null: false
      t.datetime :completed_at

      t.timestamps
    end

    add_index :csp_user_verifications,
              [:csp_user_id, :csp_verification_id],
              unique: true,
              name: "index_csp_user_verifications_on_csp_user_and_verification_id"

    add_index :csp_user_verifications, [:csp_user_id, :status]
  end

  def down
    drop_table :csp_user_verifications
  end
end
