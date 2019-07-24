# frozen_string_literal: true

FactoryBot.define do
  factory :dpc_registration do
    association :user

    organization { 'Amalgamated Lint' }
    address_1 { '1234 Shut the Door Ave.' }
    city { 'Pecoima' }
    state { 'AZ' }
    zip { '12345' }

    trait :zip_plus_4 do
      zip { '12345-6789' }
    end
  end
end
