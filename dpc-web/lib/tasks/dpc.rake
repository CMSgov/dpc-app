# frozen_string_literal: true

require './vendor/api_client/app/services/dpc_client'
require './vendor/api_client/app/serializers/organization_submit_serializer'

namespace :dpc do
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
    service.retrieve_client_token
  end
end
