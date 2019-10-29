# frozen_string_literal: true

FactoryBot.define do
  factory :profile_endpoint do
    name { 'Profile Endpoint' }
    uri { 'https://example.com/valid-endpoint' }
    connection_type { 'hl7-fhir-rest' }
    status { 'active' }
    organization { create(:organization) }
  end
end
