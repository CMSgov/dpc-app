# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    sequence(:last_name) { |n| "last_name_#{n}" }
    sequence(:first_name) { |n| "first_name_#{n}" }

    password { '123456' }
    password_confirmation { '123456' }
  end
end
