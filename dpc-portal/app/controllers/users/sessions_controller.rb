# frozen_string_literal: true

module Users
  # Adds functionality to devise session controller
  class SessionsController < Devise::SessionsController
    auto_session_timeout_actions

    def destroy
      Rails.logger.info(['User logged out',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoggedOut }])
      session['omniauth.state'] = @state = SecureRandom.hex(16)
      sign_out(current_user)
      url = URI::HTTPS.build(host: IDP_HOST,
                             path: '/openid_connect/logout',
                             query: { client_id: IDP_CLIENT_ID,
                                      post_logout_redirect_uri: "#{root_url}users/auth/logged_out",
                                      state: @state }.to_query)
      redirect_to url, allow_other_host: true
    end
  end
end
