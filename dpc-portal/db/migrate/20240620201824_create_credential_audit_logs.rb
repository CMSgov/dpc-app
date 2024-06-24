class CreateCredentialAuditLogs < ActiveRecord::Migration[7.1]
  def change
    create_table :credential_audit_logs do |t|
      t.bigint :user_id
      t.bigint :provider_organization_id, null: false
      t.integer :credential_type, null: false
      t.integer :action, null: false
      t.timestamps
      t.index :user_id
      t.index :provider_organization_id
    end
  end
end
