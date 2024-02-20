# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    password { '12345ABCDEfghi!' }
    password_confirmation { '12345ABCDEfghi!' }
  end
end
