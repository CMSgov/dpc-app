# frozen_string_literal: true

FactoryBot.define do
  factory :fhir_endpoint do
    name { 'Fhir Endpoint' }
    uri { 'https://example.com/valid-endpoint' }
    connection_type { 'hl7-fhir-rest' }
    status { 'active' }
    organization { create(:organization) }
  end
end
