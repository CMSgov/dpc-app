class AddVerificationToUsers < ActiveRecord::Migration[7.1]
  def change
    create_enum :user_verification_status, ["approved", "rejected"]
    create_enum :user_verification_reason, ["", "user_med_sanction_waived", "user_med_sanction"]

    change_table :users do |t|
        t.string :pac_id
        t.enum :verification_status, enum_type: "user_verification_status"
        t.enum :verification_reason, enum_type: "user_verification_reason"
        t.datetime :last_checked_at
    end
  end
end
