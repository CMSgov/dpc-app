# frozen_string_literal: true

FactoryBot.define do
  factory :dpc_registration do
    association :user
  end
end
