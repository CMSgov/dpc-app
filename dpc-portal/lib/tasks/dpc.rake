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
end
