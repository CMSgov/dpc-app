# frozen_string_literal: true

FactoryBot.define do
  factory :address do
    street { "15 Main St" }
    street_2 { "Suite 1" }
    city { "Fort Mill" }
    state { "SC" }
    zip { "29001" }

    addressable { build(:organization) }
  end
end
