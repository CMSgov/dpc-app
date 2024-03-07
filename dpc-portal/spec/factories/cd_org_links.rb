# frozen_string_literal: true

FactoryBot.define do
  factory :cd_org_link do
    user_id { build(:user) }
    provider_organization_id { build(:provider_organization) }
    invitation_id { build(:invitation) }
  end
end
