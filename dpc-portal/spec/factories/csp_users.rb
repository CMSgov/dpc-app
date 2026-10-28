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
    session_created_at { Time.zone.at(1_761_677_000) }
    completed_at { Time.zone.at(1_761_677_082) }

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
