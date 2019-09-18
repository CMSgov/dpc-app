class CreateTags < ActiveRecord::Migration[5.2]
  def change
    create_table :tags do |t|
      t.string :name

      t.timestamps
    end

    create_table :taggings do |t|
      t.integer :tag_id
      t.references :taggable, polymorphic: true
      t.timestamps
    end
  end
end
