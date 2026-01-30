# frozen_string_literal: true

FactoryBot.define do
  factory :idp_uid do
    provider { 'MyString' }
    uid { 'MyString' }
  end
end
