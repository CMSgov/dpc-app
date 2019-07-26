class CreateDpcRegistrations < ActiveRecord::Migration[5.2]
  def change
    create_table :dpc_registrations do |t|
      t.belongs_to :user, null: false, index: true
      t.integer :access_level, default: 0

      t.timestamps
      t.index :access_level
    end
  end
end
