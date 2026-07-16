# frozen_string_literal: true
# This file should contain all the record creation needed to seed the database with its default values.
# The data can then be loaded with the bin/rails db:seed command (or created alongside the database with db:setup).
#
# Examples:
#
#   movies = Movie.create([{ name: "Star Wars" }, { name: "Lord of the Rings" }])
#   Character.create(name: "Luke", movie: movies.first)

# Insert default rows for CSPs
Csp.find_or_create_by!(name: :login_dot_gov) { |csp| csp.start_date = Time.current }
Csp.find_or_create_by!(name: :id_me) { |csp| csp.start_date = Time.current }
Csp.find_or_create_by!(name: :clear) { |csp| csp.start_date = Time.current }
