class NullifyMedSanctionWaivedReason < ActiveRecord::Migration[8.0]
  # There's only one row in any environment (test) that has a deprecated verification reason, but we're providing
  # this anyway just in case, and to clean up local dev.

  def up
    User.where(verification_reason: 0).update_all(verification_reason: nil)
    ProviderOrganization.where(verification_reason: 0).update_all(verification_reason: nil)
  end

  def down
    raise ActiveRecord::IrreversibleMigration, 'This migration is not reversible'
  end
end
