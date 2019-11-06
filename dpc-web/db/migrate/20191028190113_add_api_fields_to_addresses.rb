class AddApiFieldsToAddresses < ActiveRecord::Migration[5.2]
  def change
    add_column :addresses, :address_use, :integer, null: false, default: 0
    add_column :addresses, :address_type, :integer, null: false, default: 0
  end
end
