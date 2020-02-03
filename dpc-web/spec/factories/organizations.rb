# frozen_string_literal: true

FactoryBot.define do
  factory :organization do
    sequence(:name) { |n| "The Health Factory #{n}" }
    organization_type { 0 }
    num_providers { 5 }
    sequence(:npi) { |n| "test-npi-#{n}" }

    after(:create) do |org|
      create(:address, addressable: org)
    end

    trait :sandbox_enabled do
      after(:create) do |org|
        create(:registered_organization, organization: org, api_env: 'sandbox')
      end
    end
  end
end
