# frozen_string_literal: true

module Users
  # Adds functionality to devise session controller
  class SessionsController < Devise::SessionsController
    auto_session_timeout_actions

    def destroy
      Rails.logger.info(['User logged out',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoggedOut }])
      sign_out(current_user)
      redirect_to url_for_login_dot_gov_logout, allow_other_host: true
    end
  end
end
