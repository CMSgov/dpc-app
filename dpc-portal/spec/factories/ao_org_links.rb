# frozen_string_literal: true

FactoryBot.define do
  factory :ao_org_link do
    user { build(:user) }
    provider_organization { build(:provider_organization) }
    invitation { build(:invitation, :ao) }
  end
end
