class AddVerificationToProviderOrganizations < ActiveRecord::Migration[7.1]
  def change
    add_column :provider_organizations, :verification_status, :integer
    add_column :provider_organizations, :verification_reason, :integer
    add_column :provider_organizations, :last_checked_at, :datetime
  end
end
