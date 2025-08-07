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

  desc <<~DESC
    Creates an organization, uploads a private key, and retrieves a client token.
    Made to address need to onboard organizations before portal released.
    Requires name, npi, public key, and signature snippet
    e.g. rails dpc:onboard NAME="Health Hut" NPI="55555" PUBLIC_KEY="--- blah blah ---" SNIPPET="base 64 thing"
  DESC
  task onboard: :environment do
    name = ENV.fetch('NAME', nil)
    npi = ENV.fetch('NPI', nil)
    public_key = ENV.fetch('PUBLIC_KEY', nil)
    snippet_signature = ENV.fetch('SNIPPET', nil)
    service = OnboardService.new(name,
                                 npi,
                                 public_key,
                                 snippet_signature)
    service.create_organization
    service.upload_key
    File.open('tmp/organization.txt', 'w') do |file|
      file.write("Organization id: \n")
      file.write(service.organization_id)
      file.write("\n\nPublic Key ID:\n")
      file.write(service.public_key_id)
      file.write("\n")
    end
    service.retrieve_client_token
    File.open('tmp/key64.txt', 'w') do |file|
      file.write(service.encrypted(service.cipher_key))
    end
    File.open('tmp/iv64.txt', 'w') do |file|
      file.write(service.encrypted(service.cipher_iv))
    end
    File.open('tmp/token64.txt', 'w') do |file|
      file.write(service.encrypted_token)
    end
  end
end
