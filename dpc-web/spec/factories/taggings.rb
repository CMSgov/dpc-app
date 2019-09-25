# frozen_string_literal: true

FactoryBot.define do
  factory :tagging do
    tag { create(:tag) }
    taggable { create(:user) }
  end
end
