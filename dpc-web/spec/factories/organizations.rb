# frozen_string_literal: true

FactoryBot.define do
  factory :organization do
    sequence(:name) { |n| "The Health Factory #{n}" }
    organization_type { 0 }
    num_providers { 5 }
    npi { generate_npi }

    after(:create) do |org|
      create(:address, addressable: org)
    end

    trait :api_enabled do
      after(:create) do |org|
        create(:registered_organization, organization: org, enabled: true)
      end
    end
  end

  def generate_npi
    loop do
      npi = Luhnacy.generate(15, prefix: '808403')[-10..-1]
      break npi unless Organization.where(npi: npi).exists?
    end
  end
end
