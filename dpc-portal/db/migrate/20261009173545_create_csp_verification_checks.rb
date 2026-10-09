class CreateCspVerificationChecks < ActiveRecord::Migration[8.0]
  def up
    create_table :csp_verification_checks do |t|
      t.references :csp_user_verification, null: false, foreign_key: true, index: true

      t.string :message_status, null: false
      t.string :description

      t.boolean :passed
      t.jsonb :reason_codes, default: [], null: false

      t.datetime :completed_at
      t.timestamps
    end
  end

  def down
    drop_table :csp_verification_checks
  end
end
