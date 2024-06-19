# frozen_string_literal: true

module Users
  # Adds auto session timeout functions to devise session controller
  class SessionsController < Devise::SessionsController
    auto_session_timeout_actions
  end
end
