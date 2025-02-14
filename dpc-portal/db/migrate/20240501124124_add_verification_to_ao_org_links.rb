class AddVerificationToAoOrgLinks < ActiveRecord::Migration[7.1]
  def change
    add_column :ao_org_links, :verification_status, :boolean, null: false, default: true
    add_column :ao_org_links, :verification_reason, :integer
    add_column :ao_org_links, :last_checked_at, :datetime, null: false, default: -> { 'CURRENT_TIMESTAMP' }
  end
end
