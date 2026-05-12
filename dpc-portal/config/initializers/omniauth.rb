# frozen_string_literal: true

# Configure omniauth providers

require "dpc_portal_utils"

include DpcPortalUtils

PORTAL_CSP_CONFIG = Rails.application.config_for(:csp).freeze
ID_ME_CONFIG = PORTAL_CSP_CONFIG[:id_me].freeze 
LOGIN_DOT_GOV_CONFIG = PORTAL_CSP_CONFIG[:login_dot_gov].freeze  

ID_ME_CLIENT_CONFIG = {
  name: :id_me,
  issuer: "https://#{ID_ME_CONFIG[:host]}/oidc",
  scope: %i[openid http://idmanagement.gov/ns/assurance/ial/2/aal/2],
  response_type: :code,
  client_auth_method: :client_secret_post,  
  client_options: {
    port:         443,
    scheme:       'https',
    host:         ID_ME_CONFIG[:host],
    identifier:   ID_ME_CONFIG[:identifier],
    secret:       ID_ME_CONFIG[:client_secret],
    redirect_uri: "#{my_protocol_host}#{ID_ME_CONFIG[:redirect_path]}",
    authorization_endpoint: ID_ME_CONFIG[:authorization_endpoint],
    token_endpoint: ID_ME_CONFIG[:token_endpoint],
    userinfo_endpoint: ID_ME_CONFIG[:user_info_endpoint],
    jwks_uri:      ID_ME_CONFIG[:jwks_uri],
    userinfo_signed_response_alg: 'RS256',
    id_token_signed_response_alg: 'RS256'
  }
}.freeze

LOGIN_DOT_GOV_CLIENT_CONFIG = {
  name: :login_dot_gov,
  issuer: "https://#{LOGIN_DOT_GOV_CONFIG[:host]}/",
  discovery: false,
  scope: %i[openid email all_emails],
  response_type: :code,
  acr_values: 'http://idmanagement.gov/ns/assurance/ial/1',
  client_auth_method: :jwt_bearer,
  client_options: {
    port: 443,
    scheme: 'https',
    host: "https://#{LOGIN_DOT_GOV_CONFIG[:host]}/",
    identifier: "urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:#{ENV['ENV']}",
    private_key: ENV['LOGIN_DOT_GOV_CLIENT_PRIVATE_KEY'],
    redirect_uri: "#{my_protocol_host}/portal/auth/login_dot_gov/callback",

    authorization_endpoint: LOGIN_DOT_GOV_CONFIG[:authorization_endpoint],
    token_endpoint: LOGIN_DOT_GOV_CONFIG[:token_endpoint],
    userinfo_endpoint: LOGIN_DOT_GOV_CONFIG[:user_info_endpoint],
    jwks_uri:      LOGIN_DOT_GOV_CONFIG[:jwks_uri],
  }
}

Rails.application.config.middleware.use OmniAuth::Builder do
  OmniAuth.config.logger = Rails.logger
  

  ## ID.me
  provider :openid_connect, ID_ME_CLIENT_CONFIG

  ## Login.gov
  provider :openid_connect, LOGIN_DOT_GOV_CLIENT_CONFIG
end
