class AddVerificationToProviderOrganizations < ActiveRecord::Migration[7.1]
  def change
    create_enum :po_verification_status, ["approved", "rejected"]
    create_enum :po_verification_reason, ["", "org_med_sanction_waived", "user_med_sanction", "no_approved_enrollments", "org_med_sanction"]

    change_table :provider_organizations do |t|
        t.enum :verification_status, enum_type: "po_verification_status"
        t.enum :verification_reason, enum_type: "po_verification_reason"
        t.datetime :last_checked_at
    end
  end
end
