# frozen_string_literal: true

# Configure omniauth providers

require "dpc_portal_utils"

include DpcPortalUtils

Rails.application.config.middleware.use OmniAuth::Builder do
  OmniAuth.config.logger = Rails.logger
  begin
    private_key = OpenSSL::PKey::RSA.new(ENV['LOGIN_GOV_PRIVATE_KEY'])
  rescue TypeError, OpenSSL::PKey::RSAError => e
    Rails.logger.error("Unable to create private key for omniauth: #{e}")
    private_key = OpenSSL::PKey::RSA.new(1024)
  end
  idp_host = ENV.fetch('IDP_HOST', 'idp.int.identitysandbox.gov')
  provider :openid_connect, {
                    name: :login_dot_gov,
                    issuer: "https://#{idp_host}/",
                    discovery: true,
                    scope: %i[openid email all_emails],
                    response_type: :code,
                    acr_values: 'http://idmanagement.gov/ns/assurance/ial/1',
                    client_auth_method: :jwt_bearer,
                    client_options: {
                      port: 443,
                      scheme: 'https',
                      host: idp_host,
                      identifier: "urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:#{ENV['ENV']}",
                      private_key: private_key,
                      redirect_uri: "#{my_protocol_host}/portal/auth/login_dot_gov/callback"
                    }
                  }
end
