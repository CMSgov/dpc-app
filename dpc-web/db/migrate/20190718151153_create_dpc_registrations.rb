class CreateDpcRegistrations < ActiveRecord::Migration[5.2]
  def change
    create_table :dpc_registrations do |t|
      t.belongs_to :user, null: false, index: true

      t.boolean :opt_in, default: false
      t.integer :opt_in_status, default: 1
      t.integer :access_level, default: 0

      t.timestamps
      t.index :opt_in
      t.index :opt_in_status
      t.index :access_level
    end
  end
end
