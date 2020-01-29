FactoryBot.define do
  factory :registered_organization do
    api_id { SecureRandom.uuid }
    api_env { 'sandbox' }

    organization

    after(:create) do |reg_org|
      create(:fhir_endpoint, registered_organization: reg_org)
    end
  end
end
