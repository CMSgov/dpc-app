# frozen_string_literal: true

FactoryBot.define do
  factory :csp do
    name { 'MyString' }
    start_date { DateTime.current - 1.year }
    end_date { nil }

    # Traits for specific CSPs
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

    # Traits for specific states
    trait :active_with_end_date do
      name { 'login_dot_gov' }
      start_date { DateTime.current - 1.year }
      end_date { DateTime.current + 1.year }
    end
    trait :inactive do
      name { 'login_dot_gov' }
      start_date { DateTime.current - 2.years }
      end_date { DateTime.current - 1.year }
    end
  end
end
