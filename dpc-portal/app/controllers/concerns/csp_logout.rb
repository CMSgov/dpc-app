# frozen_string_literal: true

# Handles CSP logout for application controllers
module CspLogout
  extend ActiveSupport::Concern

  LOGOUT_URLS = {
    'login_dot_gov' => :url_for_login_dot_gov_logout,
    'id_me' => :url_for_id_me_logout
  }.freeze

  def url_for_logout(csp)
    csp_logout_url = LOGOUT_URLS[csp.to_s]
    raise UnknownCspError, "Unknown CSP: #{csp}" unless csp_logout_url

    send(csp_logout_url)
  end

  # Documentation at https://developers.login.gov/oidc/logout/
  def url_for_login_dot_gov_logout
    session['omniauth.state'] = state = SecureRandom.hex(16)
    csp_config = CspConfig.for(:login_dot_gov)
    URI::HTTPS.build(host: csp_config.host,
                     path: csp_config.log_out_path,
                     query: { client_id: csp_config.identifier,
                              post_logout_redirect_uri: "#{root_url}auth/logged_out",
                              state: }.to_query)
  end

  def url_for_id_me_logout
    csp_config = CspConfig.for(:id_me)
    URI::HTTPS.build(host: csp_config.host,
                     path: csp_config.log_out_path,
                     query: { client_id: csp_config.identifier,
                              redirect_uri: "#{root_url}auth/logged_out" }.to_query)
  end

  class UnknownCspError < StandardError; end
end
