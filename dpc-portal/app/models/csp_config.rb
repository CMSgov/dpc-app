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
    @user_info_endpoint = config[:user_info_endpoint]
    @log_out_path = config[:log_out_path]
    @token_expiration_interval = config[:token_expiration_interval]
    @authorization_endpoint = config[:authorization_endpoint]
    @redirect_path = config[:redirect_path]
    @authorize_scope = config[:authorize_scope]
    @acr_values = config[:acr_values]
  end

  LOGIN_DOT_GOV = new('login_dot_gov',
                      CONFIG[:login_dot_gov])
  ID_ME = new('id_me',
              CONFIG[:id_me])
  #   CLEAR = new('clear',
  #               CONFIG[:clear][:host],
  #               CONFIG[:clear][:identifier],
  #               CONFIG[:clear][:user_info_path],
  #               CONFIG[:clear][:log_out_path],
  #               CONFIG[:clear][:token_expiration_interval])
  private_class_method :new

  attr_reader :code, :user_info_endpoint, :log_out_path, :token_expiration_interval, :host, :identifier,
              :authorization_endpoint, :redirect_path, :authorize_scope, :acr_values

  def self.for(code)
    case code.to_s
    when 'login_dot_gov' then LOGIN_DOT_GOV
    when 'id_me' then ID_ME
    # when 'clear' then CLEAR
    else raise ArgumentError, "Unknown CSP code: #{code}"
    end
  end

  def self.[](code)
    from(code)
  end

  def self.list
    [LOGIN_DOT_GOV.code, ID_ME.code] # CLEAR
  end
end
