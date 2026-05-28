# frozen_string_literal: true
# This file should contain all the record creation needed to seed the database with its default values.
# The data can then be loaded with the bin/rails db:seed command (or created alongside the database with db:setup).
#
# Examples:
#
#   movies = Movie.create([{ name: "Star Wars" }, { name: "Lord of the Rings" }])
#   Character.create(name: "Luke", movie: movies.first)

Csp.create!(name: :login_dot_gov, start_date: Time.current)
Csp.create!(name: :id_me, start_date: Time.current)
Csp.create!(name: :clear, start_date: Time.current)
