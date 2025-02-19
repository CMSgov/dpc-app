class CreateCredentialAuditLogs < ActiveRecord::Migration[7.1]
  def change
    create_table :credential_audit_logs do |t|
      t.bigint :user_id
      t.string :dpc_api_credential_id, null: false
      t.integer :credential_type, null: false
      t.integer :action, null: false
      t.timestamps
      t.index :user_id
      t.index :dpc_api_credential_id
    end
  end
end
