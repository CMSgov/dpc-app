# frozen_string_literal: true

FactoryBot.define do
  factory :user_sign_in do
    user
    csp
    last_login_at { Time.now }
  end
end
