# frozen_string_literal: true

# Configure omniauth providers

require "dpc_portal_utils"

include DpcPortalUtils

Rails.application.config.middleware.use OmniAuth::Builder do
  OmniAuth.config.logger = Rails.logger
  # idme stuff
  client_secret = ENV['IDP_CLIENT_SECRET']
  idp_host = ENV.fetch('IDP_HOST', 'api.idmelabs.com')
  client_id = ENV.fetch('IDP_CLIENT_ID', '925bb2985ccf623114359caa76228919')

  # clear stuff
  clear_idp_host = ENV['CLEAR_IDP_HOST']
  clear_client_id = ENV['CLEAR_IDP_CLIENT_ID']
  clear_client_secret = ENV['CLEAR_IDP_CLIENT_SECRET']

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
                      redirect_uri: "#{my_protocol_host}/auth/id_me/callback",
                      authorization_endpoint: "https://#{idp_host}/oauth/authorize",
                      token_endpoint: "https://#{idp_host}/oauth/token",
                      userinfo_endpoint: "https://#{idp_host}/api/public/v3/attributes.json",
                      jwks_uri: "https://#{idp_host}/oidc/.well-known/jwks",
                      end_session_endpoint: "https://#{idp_host}/logout"
                    }
                  }
  clear_issuer = "https://#{clear_idp_host}/integrations"
  provider :openid_connect, {
                    name: :clear,
                    issuer: clear_issuer,
                    scope: "openid",
                    response_type: :code,
                    client_auth_method: :client_secret_post,
                    client_signing_alg: :RS256,
                    client_options: {
                      port: 443,
                      scheme: 'https',
                      host: clear_idp_host,
                      identifier: clear_client_id,
                      secret: clear_client_secret,
                      redirect_uri: "#{my_protocol_host}/auth/clear/callback",
                      authorization_endpoint: "#{clear_issuer}/oauth2/auth",
                      token_endpoint: "#{clear_issuer}/oauth2/token",
                      userinfo_endpoint: "#{clear_issuer}/userinfo",
                      jwks_uri: "#{clear_issuer}/.well-known/jwks.json",
                      end_session_endpoint: "#{clear_issuer}/oauth2/sessions/logout"
                    }
                  }
end
