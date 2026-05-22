# frozen_string_literal: true

require 'erb'
# Config class to hold CSP config as defined in a config file
class CspConfig
  ENV_NAME = ENV.fetch('ENV', 'local')
  CONFIG = Rails.application.config_for(:csp).freeze

  def initialize(code, config)
    @code = code
    @host = config[:host]
    @identifier = config[:identifier]
    @client_secret = config[:client_secret]
    @client_auth_method = config[:client_auth_method]
    @authorization_endpoint = config[:authorization_endpoint]
    @token_endpoint = config[:token_endpoint]
    @user_info_endpoint = config[:user_info_endpoint]
    @jwks_uri = config[:jwks_uri]
    @redirect_path = config[:redirect_path]
    @log_out_path = config[:log_out_path]
    @token_expiration_interval = config[:token_expiration_interval]
  end

  LOGIN_DOT_GOV = new('login_dot_gov', CONFIG[:login_dot_gov])
  ID_ME = new('id_me', CONFIG[:id_me])
  CLEAR = new('clear', CONFIG[:clear])
  private_class_method :new

  attr_reader :authorization_endpoint, :client_auth_method, :client_secret, :code, :host, :identifier, :jwks_uri, :log_out_path,
              :redirect_path, :token_endpoint, :token_expiration_interval, :user_info_endpoint

  def self.for(code)
    case code.to_s
    when 'login_dot_gov' then LOGIN_DOT_GOV
    when 'id_me' then ID_ME
    when 'clear' then CLEAR
    else raise ArgumentError, "Unknown CSP code: #{code}"
    end
  end

  def self.[](code)
    self.for(code)
  end

  def self.list
    [LOGIN_DOT_GOV.code, ID_ME.code, CLEAR.code]
  end
end
