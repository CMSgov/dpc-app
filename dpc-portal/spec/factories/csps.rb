# frozen_string_literal: true

FactoryBot.define do
  factory :csp do
    name { 'MyString' }
    start_date { '2026-04-24 19:07:38' }
    end_date { '2026-04-24 19:07:38' }

    trait :login_dot_gov do
      name { 'login_dot_gov' }
      start_date { '2026-04-24 19:07:38' }
    end
    trait :id_me do
      name { 'id_me' }
      start_date { '2026-04-24 19:07:38' }
    end
    trait :clear do
      name { 'clear' }
      start_date { '2026-04-24 19:07:38' }
    end
  end
end
