# frozen_string_literal: true

# Configure omniauth providers

require "dpc_portal_utils"

include DpcPortalUtils

Rails.application.config.middleware.use OmniAuth::Builder do
  OmniAuth.config.logger = Rails.logger
  idp_host = ENV.fetch('IDP_HOST', 'api.idmelabs.com')
  client_id = ENV.fetch('IDP_CLIENT_ID', '925bb2985ccf623114359caa76228919')
  client_secret = ENV['IDP_CLIENT_SECRET']
  provider :oidc, {
                    name: :id_me,
                    issuer: "https://#{idp_host}/oidc",
                    scope: %i[openid http://idmanagement.gov/ns/assurance/ial/2/aal/2],
                    response_type: :code,
                    client_auth_method: :client_secret_post,
                    fetch_user_info: false,
                    require_state: false,
                    client_options: {
                      port: 443,
                      scheme: 'https',
                      host: idp_host,
                      identifier: client_id,
                      secret: client_secret,
                      config_endpoint: "https://#{idp_host}/oidc/.well-known/openid-configuration",
                      end_session_endpoint: "https://#{idp_host}/id_me/logout"
                    }
                  }
end
