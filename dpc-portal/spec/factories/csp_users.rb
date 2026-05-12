# frozen_string_literal: true

FactoryBot.define do
  factory :csp_user do
    user { nil }
    csp { nil }
    uuid { '' }
  end
end
