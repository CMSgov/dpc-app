class DropDpcRegistrations < ActiveRecord::Migration[5.2]
  def change
    drop_table :dpc_registrations
  end
end
