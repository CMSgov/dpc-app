# frozen_string_literal: true

FactoryBot.define do
  factory :csp_user do
    user { nil }
    csp { nil }
    uuid { '' }
  end

  factory :csp_user_verification do
    csp { Csp.find_by(name: 'clear') || association(:csp, :clear) }
    sequence(:csp_verification_id) { |n| "verify_factory#{n}" }
    status { 'success' }
    created_at { 2.hours.ago }
    completed_at { 1.hour.ago }

    trait :failed do
      status { 'fail' }
    end
  end

  factory :csp_verification_check do
    csp_user_verification
    check_name { 'Gov ID is not expired' }
    check_status { 'completed' }
    status_value { true }
    reason_codes { [] }
  end
end
