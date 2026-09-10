# frozen_string_literal: true

module Users
  # Handles session destruction
  class SessionsController < ApplicationController
    auto_session_timeout_actions

    def destroy
      csp = session[:csp]
      current_user_identifier = current_csp_user_identifier
      clean_session
      log_event(:info, 'User logged out',
                action_context: LoggingConstants::ActionContext::Authentication,
                action_type: LoggingConstants::ActionType::UserLoggedOut,
                user_identifier: current_user_identifier,
                csp: csp)
      redirect_to url_for_logout(csp), allow_other_host: true
    end

    def logged_out
      clean_session
      redirect_to session.delete(:user_return_to) || sign_in_path
    end

    def clean_session
      csp_session.clear_user
      csp_session.clear_all
    end
  end
end
