FactoryBot.define do
  factory :registered_organization do
    api_id { SecureRandom.uuid }

    organization
  end
end
