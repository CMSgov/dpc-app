# frozen_string_literal: true

FactoryBot.define do
  factory :internal_user do
    sequence(:email) { |n| "user#{n}@example.com" }

    password { '123456' }
    password_confirmation { '123456' }
  end
end
