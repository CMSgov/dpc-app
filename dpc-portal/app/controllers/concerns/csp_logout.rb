# frozen_string_literal: true

# Handles CSP logout for application controllers
module CspLogout
  extend ActiveSupport::Concern

  def url_for_logout(csp)
    case csp.to_s
    when 'id_me'         then url_for_id_me_logout
    when 'login_dot_gov' then url_for_login_dot_gov_logout
    else                 raise UnknownCspError, "Unknown CSP: #{csp}"
    end
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
                              redirect_uri: "#{root_url}oauth/logged_out" }.to_query)
  end

  class UnknownCspError < StandardError; end
end
