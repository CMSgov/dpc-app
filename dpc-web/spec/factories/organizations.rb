# frozen_string_literal: true

require "./lib/luhnacy_lib/luhnacy_lib"

FactoryBot.define do
  factory :organization do
    sequence(:name) { |n| "The Health Factory #{n}" }
    organization_type { 0 }
    num_providers { 5 }
    npi {
      loop do
        npi = LuhnacyLib.generate_npi
        break npi unless Organization.where(npi:).exists?
      end
    }

    after(:create) do |org|
      create(:address, addressable: org)
    end

    trait :api_enabled do
      after(:create) do |org|
        create(:registered_organization, organization: org, enabled: true)
      end
    end
  end
end
