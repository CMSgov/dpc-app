FactoryBot.define do
  factory :cd_org_link do
    user_id { 1 }
    provider_organization_id { 1 }
    invitation_id { 1 }
    disabled_at { nil }
  end
end
