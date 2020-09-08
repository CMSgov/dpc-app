class RemoveTheResources < ActiveRecord::Migration[6.0]
  def change
    drop_table :the_resources
  end
end
