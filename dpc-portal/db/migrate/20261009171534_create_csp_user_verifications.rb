class CreateCspUserVerifications < ActiveRecord::Migration[8.0]
  def up
    create_table :csp_user_verifications do |t|
      t.references :invitation, null: true, foreign_key: true, index: true
      t.references :user, null: true, foreign_key: true, index: true
      t.references :csp, null: false, foreign_key: true, index: true

      t.string :csp_verification_id, null: false
      t.string :status, null: false
      t.datetime :completed_at

      t.timestamps
    end

    add_index :csp_user_verifications,
              [:csp_id, :csp_verification_id],
              unique: true,
              name: "index_csp_user_verifications_on_csp_and_verification_id"

    add_index :csp_user_verifications, [:user_id, :status]
    add_index :csp_user_verifications, [:invitation_id, :status]
  end

  def down
    drop_table :csp_user_verifications
  end
end