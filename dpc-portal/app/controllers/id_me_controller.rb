# frozen_string_literal: true

# Handles interactions with ID.me
class IdMeController < CspController
  def csp_code     = :id_me
  def display_name = 'ID.me'

  def ial_1_user?(auth)
    auth.extra.raw_info.identity_assurance_level.to_i == 1
  end

  def all_emails(auth)
    auth.extra.raw_info.emails_confirmed
  end
end
