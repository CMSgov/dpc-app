# frozen_string_literal: true

require './vendor/api_client/app/services/dpc_client'
require './vendor/api_client/app/serializers/organization_submit_serializer'

namespace :dpc do
  desc <<~DESC
    Create an Invitation for an Authorized Official
    provide comma-separated values in INVITE: given name, family name, email, org npi
    e.g. rails dpc:invite_ao INVITE=Bob,Hoskins,bob@example.com,11111111111
  DESC
  task invite_ao: :environment do
    ao_given_name, ao_family_name, ao_email, org_npi = ENV['INVITE'].split(',')
    service = AoInvitationService.new
    invitation = service.create_invitation(ao_given_name, ao_family_name, ao_email, org_npi)
    puts "Invitation created for #{ao_given_name} #{ao_family_name} for #{invitation.provider_organization.name}"
    if Rails.env.development?
      puts "http://localhost:3100/portal/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/accept"
    end
  rescue AoInvitationServiceError => e
    puts "Unable to create invitation: #{e.message}"
  end

  desc 'Create a new organization with random attributes. Prints link.'
  task :make_org do
    raise BadEnvironmentError, 'Only for local development' unless ENV['ENV'] == 'local'

    org = MockOrg.new
    fhir_endpoint = { 'status' => 'test',
                      'name' => 'DPC Sandbox Test Endpoint',
                      'uri' => 'https://dpc.cms.gov/test-endpoint' }

    client = DpcClient.new
    client.create_organization(org, fhir_endpoint:)
    if client.response_successful?
      puts "Organization NPI: #{client.response_body['identifier'][0]['value']}"
      puts "Organization Page: http://localhost:3100/portal/organizations/#{client.response_body['id']}"
    else
      puts "HTTP ERROR #{client.response_status} creating org"
    end
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
