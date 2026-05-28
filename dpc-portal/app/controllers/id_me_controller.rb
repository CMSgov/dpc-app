# frozen_string_literal: true

# Handles interactions with ID.me
class IdMeController < CspController
  def name         = :id_me
  def display_name = 'ID.me'

  def ial_1_user?(auth)
    auth.extra.raw_info.identity_assurance_level == 1
  end

  def user_emails(auth)
    [auth.info.email]
  end
end
