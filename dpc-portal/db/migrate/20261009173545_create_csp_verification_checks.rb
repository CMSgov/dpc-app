class CreateCspVerificationChecks < ActiveRecord::Migration[8.0]
  def up
    create_table :csp_verification_checks do |t|
      t.references :csp_user_verification, null: false, foreign_key: true, index: true

      t.string :check_name, null: false
      t.string :check_status

      t.boolean :status_value
      t.integer :status_code
      t.string :status_message

      t.jsonb :reason_codes, default: [], null: false

      t.datetime :completed_at
      t.timestamps
    end
  end

  def down
    drop_table :csp_verification_checks
  end
end
