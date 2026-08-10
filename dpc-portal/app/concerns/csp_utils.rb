# frozen_string_literal: true

# Shared CSP mapping function
module CspUtils
  extend ActiveSupport::Concern

  CODES_TO_DISPLAY = {
    login_dot_gov: 'Login.gov',
    id_me: 'ID.me',
    clear: 'CLEAR'
  }.freeze

  def self.display_name(csp_code)
    CODES_TO_DISPLAY.fetch(csp_code.to_sym)
  end
end
