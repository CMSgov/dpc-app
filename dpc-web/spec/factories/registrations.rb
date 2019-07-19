# frozen_string_literal: true

FactoryBot.define do
  factory :registration do
    association :user

    organization { 'Amalgamated Lint' }
    address_1 { '1234 Shut the Door Ave.' }
    city { 'Pecoima' }
    state { 'AZ' }
    zip { '12345-1234' }
  end
end
