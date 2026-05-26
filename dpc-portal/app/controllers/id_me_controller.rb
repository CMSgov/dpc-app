# frozen_string_literal: true

# Handles interactions with ID.me
class IdMeController < CspController
  private

  def name         = :id_me
  def display_name = 'ID.me'

  def logout_url
    state = SecureRandom.hex(16)
    session['omniauth.state'] = state
    client_options = ID_ME_CLIENT_CONFIG[:client_options]
    URI::HTTPS.build(host: client_options[:host],
                     path: client_options[:log_out_path],
                     query: { client_id: client_options[:identifier],
                              redirect_uri: "#{root_url}oauth/logged_out" }.to_query)
  end

  def ial_1?(auth)
    auth.extra.raw_info.identity_assurance_level == 1
  end

  def ial_2_actions(user, auth)
    data = auth.extra.raw_info
    return unless data.identity_assurance_level == 2

    maybe_update_user(user, data)
    session[:login_token] = auth.credentials.token
    session[:login_token_exp] = auth.credentials.expires_in.seconds.from_now
  end

  def user_emails(auth)
    [auth.info.email]
  end
end
