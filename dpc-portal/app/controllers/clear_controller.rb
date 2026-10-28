# frozen_string_literal: true

# Handles interactions with CLEAR
class ClearController < CspController
  def csp_code     = :clear
  def display_name = 'CLEAR'

  def all_emails(auth)
    [auth.extra.raw_info.email]
  end

  def store_id_token? = true
end
