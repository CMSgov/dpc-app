# frozen_string_literal: true

FactoryBot.define do
  factory :user do
    sequence(:email) { |n| "user#{n}@example.com" }
    sequence(:last_name) { |n| "last_name_#{n}" }
    sequence(:first_name) { |n| "first_name_#{n}" }

    requested_organization { 'Amalgamated Lint' }
    requested_organization_type { 'primary_care_clinic' }

    address_1 { '1234 Shut the Door Ave.' }
    city { 'Pecoima' }
    state { 'AZ' }
    zip { '12345' }
    agree_to_terms { true }

    password { '123456' }
    password_confirmation { '123456' }

    trait :zip_plus_4 do
      zip { '12345-6789' }
    end

    trait :vendor do
      after(:create) do |user, evaluator|
        vendor = create(:organization, organization_type: 'health_it_vendor')
        user.organizations << vendor
      end
    end

    trait :assigned do
      after(:create) do |user, evaluator|
        organization = create(:organization, organization_type: 'urgent_care')
        user.organizations << organization
      end
    end
  end
end
