# frozen_string_literal: true

FactoryBot.define do
  factory :user, aliases: %i[invited_by] do
    sequence(:uid) { |n| n }
    email { "user#{rand(0..100_000)}@example.com" }
  end
end
