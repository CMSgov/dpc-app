class AddConfigCompleteToProviderOrganization < ActiveRecord::Migration[7.2]
  def change
    add_column :provider_organizations, :config_complete, :boolean, default: false
  end
end
