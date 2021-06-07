# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    sequence(:last_name) { |n| "last_name_#{n}" }
    sequence(:first_name) { |n| "first_name_#{n}" }
    sequence(:implementer) { |n| "Enterprise_#{n}" }

    implementer_id { SecureRandom.uuid }

    agree_to_terms { true }

    confirmation_sent_at { DateTime.now }
    confirmed_at { DateTime.now }

    password { '12345ABCDEfghi!' }
    password_confirmation { '12345ABCDEfghi!' }
  end
end
