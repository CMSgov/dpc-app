# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    sequence(:last_name) { |n| "last_name_#{n}" }
    sequence(:first_name) { |n| "first_name_#{n}" }

    organization { 'Amalgamated Lint' }
    address_1 { '1234 Shut the Door Ave.' }
    city { 'Pecoima' }
    state { 'AZ' }
    zip { '12345' }
    organization_type { 0 }

    password { '123456' }
    password_confirmation { '123456' }

    trait :zip_plus_4 do
      zip { '12345-6789' }
    end
  end
end
