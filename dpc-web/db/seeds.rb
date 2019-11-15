# This file should contain all the record creation needed to seed the database with its default values.
# The data can then be loaded with the rails db:seed command (or created alongside the database with db:setup).
#
# Examples:
#
#   movies = Movie.create([{ name: 'Star Wars' }, { name: 'Lord of the Rings' }])
#   Character.create(name: 'Luke', movie: movies.first)

wash = FactoryBot.create(:user, email: 'washirv@example.com', first_name: 'Washington', last_name: 'Irving')
jane = FactoryBot.create(:user, email: 'janeaus@example.com', first_name: 'Jane', last_name: 'Austen')

org = FactoryBot.create(:organization, name: 'Hollow Health')
wash.organizations << org

tag = FactoryBot.create(:tag, name: 'Contacted')
jane.tags << tag