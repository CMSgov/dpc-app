# frozen_string_literal: true

# Shared CSP mapping functions
module CspUtils
  extend ActiveSupport::Concern

  CODES_TO_DISPLAY = {
    login_dot_gov: 'Login.gov',
    id_me: 'ID.me',
    clear: 'CLEAR'
  }.freeze

  CODES_TO_CONFIGS = {
    login_dot_gov: LOGIN_DOT_GOV_CLIENT_CONFIG,
    id_me: ID_ME_CLIENT_CONFIG,
    clear: CLEAR_CLIENT_CONFIG
  }.freeze

  def self.display_name(csp_code)
    CODES_TO_DISPLAY.fetch(csp_code.to_sym)
  end

  def self.user_info_url(csp_code)
    config = CODES_TO_CONFIGS.fetch(csp_code.to_sym) { raise ArgumentError, "Unknown CSP code: #{csp_code}" }
    config[:client_options][:userinfo_endpoint]
  end
end
