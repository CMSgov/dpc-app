class CreateTheResources < ActiveRecord::Migration[6.0]
  def change
    drop_table :the_resources

    create_table :the_resources do |t|
      t.datetime :password_changed_at
    end
    add_index :the_resources, :password_changed_at
  end
end
