# frozen_string_literal: true

FactoryBot.define do
  factory :tag do
    sequence(:name) { |n| "MyTag-#{n}" }
  end
end
