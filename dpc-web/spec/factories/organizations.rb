# frozen_string_literal: true

FactoryBot.define do
  factory :organization do
    sequence(:name) { |n| "The Health Factory #{n}" }
    organization_type { 0 }
    num_providers { 5 }
  end
end
