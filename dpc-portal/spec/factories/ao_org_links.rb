# frozen_string_literal: true

FactoryBot.define do
  factory :ao_org_link do
    user { build(:user) }
    provider_organization { build(:provider_organization, dpc_api_organization_id: SecureRandom.uuid) }
    invitation { build(:invitation, :ao, provider_organization:) }
  end
end
