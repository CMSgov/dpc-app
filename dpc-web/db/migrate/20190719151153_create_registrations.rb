class CreateRegistrations < ActiveRecord::Migration[5.2]
  def change
    create_table :registrations do |t|
      t.belongs_to :user, null: false, index: true

      t.string :organization, null: false
      t.string :address_1, null: false
      t.string :address_2, default: ''
      t.string :city, null: false
      t.string :state, null: false
      t.string :zip, null: false
      t.boolean :opt_in, default: true

      t.timestamps
      t.index :organization
    end
  end
end
