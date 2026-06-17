# frozen_string_literal: true

# Handles interactions with login.gov.
class LoginDotGovController < CspController
  def csp_code     = :login_dot_gov
  def display_name = 'Login.gov'

  def ial_1_user?(auth)
    auth.extra.raw_info.ial == 'http://idmanagement.gov/ns/assurance/ial/1'
  end
end
