# frozen_string_literal: true

module Users
  class SessionsController < ApplicationController
    def active
      render_session_status
    end

    def timeout
      render_session_timeout
    end
    def create
      user_info = request.env['omniauth.auth']
      if user_info.provider == 'developer'
        user = User.where(email: user_info.info.email).first
        session['user'] = user.id if user
      end
      render plain: :foo
    end
    def destroy
      Rails.logger.info(['User logged out',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoggedOut }])
      session.delete('user')
      redirect_to url_for_login_dot_gov_logout, allow_other_host: true
    end
  end
end
