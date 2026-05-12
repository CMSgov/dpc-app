# frozen_string_literal: true

require 'erb'
# Config class to hold CSP config as defined in a config file
class CspConfig
  ENV_NAME = ENV.fetch('ENV', 'local')
  CONFIG = Rails.application.config_for(:csp).freeze

  def initialize(code, user_info_endpoint, log_out_path, token_expiration_interval)
    @code = code
    @user_info_endpoint = user_info_endpoint
    @log_out_path = log_out_path
    @token_expiration_interval = token_expiration_interval
  end

  LOGIN_DOT_GOV = new('login_dot_gov',
                      CONFIG['login_dot_gov']['user_info_path'],
                      CONFIG['login_dot_gov']['log_out_path'],
                      CONFIG['login_dot_gov']['token_expiration_interval'])
  ID_ME = new('id_me',
              CONFIG['id_me']['user_info_path'],
              CONFIG['id_me']['log_out_path'],
              CONFIG['id_me']['token_expiration_interval'])
  #   CLEAR = new('clear',
  #               CONFIG['clear']['user_info_path'],
  #               CONFIG['clear']['log_out_path'],
  #               CONFIG['clear']['token_expiration_interval'])
  private_class_method :new

  attr_reader :user_info_endpoint

  def logout_uri
    @log_out_path
  end

  def self.from(code)
    case code.to_s
    when 'LOGIN_DOT_GOV' then LOGIN_DOT_GOV
    when 'ID_ME' then ID_ME
    # when 'CLEAR' then CLEAR
    else raise ArgumentError, "Unknown CSP code: #{code}"
    end
  end

  def self.[](code)
    from(code)
  end
end
