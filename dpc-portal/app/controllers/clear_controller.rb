# frozen_string_literal: true

# Handles interactions with ID.me
class ClearController < CspController
  def csp_code     = :clear
  def display_name = 'CLEAR'

  def update_csp_tokens(auth)
    super
    session["clear_id_token"] = auth.credentials.id_token # required for CLEAR logout
  end

  def all_emails(auth)
    [auth.extra.raw_info.email]
  end
end
