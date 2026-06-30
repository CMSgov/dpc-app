# frozen_string_literal: true

FactoryBot.define do
  factory :user, aliases: %i[invited_by] do
    sequence(:uid) { |n| n }
    provider { :id_me }
    sequence(:email) { |n| "user#{n}@example.com" }
  end
end
