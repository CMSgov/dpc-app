# frozen_string_literal: true

FactoryBot.define do
  factory :admin do
    sequence(:email) { |n| "admin#{n}@example.com" }

    password { '123456' }
    password_confirmation { '123456' }
  end
end
