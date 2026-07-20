# frozen_string_literal: true

# Configure omniauth providers

require 'json'
require "dpc_portal_utils"

include DpcPortalUtils

PORTAL_CSP_CONFIG = Rails.application.config_for(:csp).freeze
ID_ME_CONFIG = PORTAL_CSP_CONFIG[:id_me].freeze
LOGIN_DOT_GOV_CONFIG = PORTAL_CSP_CONFIG[:login_dot_gov].freeze
CLEAR_CONFIG = PORTAL_CSP_CONFIG[:clear].freeze
CLEAR_OIDC_CLAIMS = {
  id_token: {
    ssn9: nil,
    email: nil,
    email_verified: nil,
    given_name: nil,
    family_name: nil
  },
  userinfo: {
    ssn9: nil,
    email: nil,
    email_verified: nil,
    given_name: nil,
    family_name: nil
  }
}.freeze
CLEAR_OIDC_CLAIMS_PARAM = JSON.generate(CLEAR_OIDC_CLAIMS).freeze

## Build Login.gov RSA private key object before defining the config constant
LOGIN_DOT_GOV_PRIVATE_KEY = begin
  OpenSSL::PKey::RSA.new(ENV['LOGIN_DOT_GOV_CLIENT_PRIVATE_KEY']&.gsub('\\n', "\n"))
rescue TypeError, OpenSSL::PKey::RSAError => e
  Rails.logger.error("Unable to create Login.gov private key for omniauth: #{e}")
  OpenSSL::PKey::RSA.new(1024)
end

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
    token_endpoint:         ID_ME_CONFIG[:token_endpoint],
    userinfo_endpoint:      ID_ME_CONFIG[:user_info_endpoint],
    jwks_uri:               ID_ME_CONFIG[:jwks_uri],
    userinfo_signed_response_alg: 'RS256',
    id_token_signed_response_alg: 'RS256'
  }
}.freeze

LOGIN_DOT_GOV_CLIENT_CONFIG = {
  name: :login_dot_gov,
  issuer: "https://#{LOGIN_DOT_GOV_CONFIG[:host]}/",
  discovery: true,
  scope: %i[openid email all_emails],
  response_type: :code,
  acr_values: 'http://idmanagement.gov/ns/assurance/ial/2',
  client_auth_method: :jwt_bearer,
  client_options: {
    port:        443,
    scheme:      'https',
    host:        LOGIN_DOT_GOV_CONFIG[:host],
    identifier:  "urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:#{ENV['ENV']}",
    private_key: LOGIN_DOT_GOV_PRIVATE_KEY,
    redirect_uri: "#{my_protocol_host}/auth/login_dot_gov/callback",
    authorization_endpoint: LOGIN_DOT_GOV_CONFIG[:authorization_endpoint],
    token_endpoint:         LOGIN_DOT_GOV_CONFIG[:token_endpoint],
    userinfo_endpoint:      LOGIN_DOT_GOV_CONFIG[:user_info_endpoint],
    jwks_uri:               LOGIN_DOT_GOV_CONFIG[:jwks_uri]
  }
}.freeze

CLEAR_CLIENT_CONFIG = {
  name: :clear,
  issuer: "https://#{CLEAR_CONFIG[:host]}/integrations",
  scope: 'openid',
  response_type: :code,
  client_auth_method: :client_secret_post,
  client_signing_alg: :RS256,
  client_options: {
    port: 443,
    scheme: 'https',
    host: CLEAR_CONFIG[:host],
    identifier: CLEAR_CONFIG[:identifier],
    secret: CLEAR_CONFIG[:client_secret],
    redirect_uri: "#{my_protocol_host}#{CLEAR_CONFIG[:redirect_path]}",
    authorization_endpoint: CLEAR_CONFIG[:authorization_endpoint],
    token_endpoint: CLEAR_CONFIG[:token_endpoint],
    userinfo_endpoint: CLEAR_CONFIG[:user_info_endpoint],
    jwks_uri: CLEAR_CONFIG[:jwks_uri],
    end_session_endpoint: "https://#{CLEAR_CONFIG[:host]}#{CLEAR_CONFIG[:log_out_path]}"
  }
}.freeze


Rails.application.config.middleware.use OmniAuth::Builder do
  OmniAuth.config.logger = Rails.logger

  ## CLEAR
  provider :openid_connect, CLEAR_CLIENT_CONFIG

  ## ID.me
  provider :openid_connect, ID_ME_CLIENT_CONFIG

  ## Login.gov
  provider :openid_connect, LOGIN_DOT_GOV_CLIENT_CONFIG
end
