# frozen_string_literal: true

# Handles interactions with login.gov.
class LoginDotGovController < CspController
  def csp_code     = :login_dot_gov
  def display_name = 'Login.gov'
end
