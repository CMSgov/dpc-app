# frozen_string_literal: true

module Users
  # Handles session destruction
  class SessionsController < ApplicationController
    auto_session_timeout_actions

    def destroy
      Rails.logger.info(['User logged out',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoggedOut }])
      session.delete('user')
      csp = session.delete(:csp)
      session.delete("#{csp}_token") if csp
      session.delete("#{csp}_token_exp") if csp

      redirect_to url_for_logout(csp), allow_other_host: true
    end

    def logged_out
      redirect_to session.delete(:user_return_to) || sign_in_path
    end
  end
end
