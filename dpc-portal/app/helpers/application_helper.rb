# frozen_string_literal: true

# Utility methods for views
module ApplicationHelper
  def current_user
    @current_user
  end

  def omniauth_authorize_path(service)
    "/portal/auth/#{service}"
  end
end
