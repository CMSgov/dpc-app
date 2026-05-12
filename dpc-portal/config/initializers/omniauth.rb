# frozen_string_literal: true

# Configure omniauth providers

require "dpc_portal_utils"

include DpcPortalUtils

Rails.application.config.middleware.use OmniAuth::Builder do
  OmniAuth.config.logger = Rails.logger
  # idp_host = ENV.fetch('IDP_HOST', 'api.idmelabs.com')
  idp_host = ENV.fetch('CLEAR_IDP_HOST')
  # client_id = ENV.fetch('IDP_CLIENT_ID', '925bb2985ccf623114359caa76228919')
  client_id = ENV.fetch('CLEAR_IDP_CLIENT_ID')
  # client_secret = ENV['IDP_CLIENT_SECRET']
  client_secret = ENV['CLEAR_IDP_CLIENT_SECRET']
  provider :openid_connect, {
                    name: :id_me,
                    issuer: "https://#{idp_host}/oidc",
                    scope: %i[openid http://idmanagement.gov/ns/assurance/ial/2/aal/2],
                    response_type: :code,
                    client_auth_method: :client_secret_post,
                    client_options: {
                      port: 443,
                      scheme: 'https',
                      host: idp_host,
                      identifier: client_id,
                      secret: client_secret,
                      # redirect_uri: "#{my_protocol_host}/auth/id_me/callback",
                      redirect_uri: "#{my_protocol_host}/auth/clear/callback",
                      # authorization_endpoint: "https://#{idp_host}/oauth/authorize",
                      authorization_endpoint: "https://#{idp_host}/integrations/oauth2/auth",
                      # token_endpoint: "https://#{idp_host}/oauth/token",
                      token_endpoint: "https://#{idp_host}/integrations/oauth2/token",
                      userinfo_endpoint: "https://#{idp_host}/api/public/v3/attributes.json",
                      jwks_uri: "https://#{idp_host}/oidc/.well-known/jwks",
                      end_session_endpoint: "https://#{idp_host}/logout"
                    }
                  }
end
