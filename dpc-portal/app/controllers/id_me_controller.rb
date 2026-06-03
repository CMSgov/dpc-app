# frozen_string_literal: true

# Handles interactions with ID.me
class IdMeController < CspController
  def name         = :id_me
  def display_name = 'ID.me'

  def ial_1_user?(auth)
    auth.extra.raw_info.identity_assurance_level.to_i == 1
  end

  # ID.me only provides the primary email
  def all_emails(auth)
    [auth.info.email]
  end
end
