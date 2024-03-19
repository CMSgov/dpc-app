# frozen_string_literal: true

FactoryBot.define do
  factory :provider_organization do
    npi { 10.times.map { rand(0..9) }.join }
  end
end
