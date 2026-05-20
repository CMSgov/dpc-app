class CreateCsps < ActiveRecord::Migration[8.0]
  # Local version of the model to bypass the main one's readonly status
  class Csp < ApplicationRecord; end

  def change
    create_table :csps do |t|
      t.string :name
      t.datetime :start_date
      t.datetime :end_date
    end

    # Insert default rows
    Csp.create!(name: :login_dot_gov, start_date: Time.current)
    Csp.create!(name: :id_dot_me, start_date: Time.current)
    Csp.create!(name: :clear, start_date: Time.current)
  end
end
