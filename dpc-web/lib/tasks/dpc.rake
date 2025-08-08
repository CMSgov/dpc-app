# frozen_string_literal: true

require 'zip'
require './vendor/api_client/app/services/dpc_client'
require './vendor/api_client/app/serializers/organization_submit_serializer'

# rubocop:disable Metrics/BlockLength
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
    ips = ENV.fetch('IPS', nil)
    service = OnboardService.new(name,
                                 npi,
                                 public_key,
                                 snippet_signature,
                                 ips)
    service.create_organization
    service.retrieve_client_token
    service.upload_ips
    service.upload_key

    password = SecureRandom.base64(128)
    Zip::TraditionalEncrypter.new(password)
    buffer = Zip::OutputStream.write_buffer(encrypter: enc) do |output|
      output.put_next_entry('dpc-credentials.txt')
      output.write("Registered Organization ID: #{service.organization_id}\n\n")
      output.write("Public Key ID: #{service.public_key_id}\n\n")
      output.write("Organization Token Expiration Date: #{1.year.from_now.strftime('%B %e, %Y')}\n\n")
      output.write("Organization Client Token:\n")
      output.write(service.client_token)
      output.write("\n")
    end

    File.binwrite('dpc-credentials.zip', buffer.string)
    password_enc = service.encrypted(password)
    File.binwrite('password.enc', password_enc)
  end
end
# rubocop:enable Metrics/BlockLength
