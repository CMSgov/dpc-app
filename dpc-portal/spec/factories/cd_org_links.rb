# frozen_string_literal: true

FactoryBot.define do
  factory :cd_org_link do
    user { build(:user) }
    provider_organization { build(:provider_organization) }
    invitation { build(:invitation) }
  end
end
