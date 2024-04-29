class AddVerificationToAoOrgLinks < ActiveRecord::Migration[7.1]
  def change
    create_enum :ao_org_link_verification_reason, ["ao_removal", "user_med_sanction", "no_approved_enrollments", "org_med_sanction"]

    change_table :ao_org_links do |t|
        t.boolean :verification_status, null: false, default: true
        t.enum :verification_reason, enum_type: "ao_org_link_verification_reason"
        t.datetime :last_checked_at, null: false, default: -> { 'CURRENT_TIMESTAMP' }
    end
  end
end
