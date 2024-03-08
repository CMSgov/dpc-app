class CreateProviderOrganizations < ActiveRecord::Migration[7.0]
  def change
    create_table :provider_organizations do |t|
      t.string :dpc_api_organization_id
      t.string :name
      t.string :npi, null: false
      t.bigint :terms_of_service_accepted_by_id
      t.datetime :terms_of_service_accepted_at
      t.timestamps
    end

    add_foreign_key :provider_organizations, :users, column: :terms_of_service_accepted_by_id
    add_index :provider_organizations, :dpc_api_organization_id, unique: true
    add_index :provider_organizations, :npi, unique: true
  end
end
