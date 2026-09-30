# frozen_string_literal: true

# Shared CSP mapping functions
module CspUtils
  extend ActiveSupport::Concern

  class MultiUserMatchError < StandardError; end

  class NameMismatchError < StandardError; end

  class SsnMismatchError < StandardError; end

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

  def self.lookup_by_host(host)
    CODES_TO_CONFIGS.each do |csp_code, config|
      return csp_code if config[:client_options][:host] == host
    end
    nil
  end

  def self.issuer(host)
    csp = lookup_by_host(host)
    raise ArgumentError, "Unknown CSP host: #{host}" unless csp

    CODES_TO_CONFIGS.fetch(csp)[:issuer]
  end
end
