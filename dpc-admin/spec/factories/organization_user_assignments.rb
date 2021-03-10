# frozen_string_literal: true

FactoryBot.define do
  factory :organization_user_assignment do
    user
    organization
  end
end
