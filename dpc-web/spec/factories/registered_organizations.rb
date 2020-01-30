FactoryBot.define do
  factory :registered_organization do
    api_id { SecureRandom.uuid }
    api_env { 'sandbox' }
    api_endpoint_ref { "Endpoint/#{SecureRandom.uuid}" }

    organization

    before(:create) do |reg_org|
      reg_org.build_default_fhir_endpoint
    end
  end
end
