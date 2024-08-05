# frozen_string_literal: true

module Users
  # Adds functionality to devise session controller
  class SessionsController < Devise::SessionsController
    auto_session_timeout_actions

    def destroy
      logger.info('User logged out',
                  actionContext: LoggingConstants::ActionContext::Authentication,
                  actionType: LoggingConstants::ActionType::UserLoggedOut)
      super
    end
  end
end
