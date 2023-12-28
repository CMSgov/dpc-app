# frozen_string_literal: true

require './vendor/api_client/app/services/dpc_client'
require './vendor/api_client/app/serializers/organization_submit_serializer'
namespace :dpc do
  desc 'Create a new organization with random attributes. Prints link.'
  task :make_org do
    raise BadEnvironmentError, 'Only for local development' unless ENV['ENV'] == 'local'

    org = MockOrg.new
    fhir_endpoint = { 'status' => 'test',
                      'name' => 'DPC Sandbox Test Endpoint',
                      'uri' => 'https://dpc.cms.gov/test-endpoint' }
    client = DpcClient.new
    client.create_organization(org, fhir_endpoint: fhir_endpoint)
    puts "http://localhost:3100/portal/organizations/#{client.response_body['id']}"
  end
end

# Fakes an org necessary to work with the DpcClient
class MockOrg
  # rubocop:disable Naming/VariableNumber
  attr_reader :npi, :name, :address_use, :address_type, :address_city, :address_state, :address_street,
              :address_street_2, :address_zip

  def initialize
    randy = Random.new
    @name = "Generated Organization #{randy.rand(1000)}"
    @npi = Luhnacy.doctor_npi[-10..]
    @address_use = 'work'
    @address_type = 'both'
    @address_street = "#{randy.rand(1000)} Elm St"
    @address_street_2 = "Suite #{randy.rand(100)}"
    @address_city = 'Akron'
    @address_state = 'OH'
    @address_zip = '22222'
  end
  # rubocop:enable Naming/VariableNumber
end

class BadEnvironmentError < StandardError; end
