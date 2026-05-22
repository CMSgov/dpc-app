# frozen_string_literal: true

FactoryBot.define do
  factory :user_email do
    csp_user { nil }
    email { 'MyString' }
    active { false }
    deactivated_at { '2026-04-24 19:40:06' }
    reactivated_at { '2026-04-24 19:40:06' }
  end
end
