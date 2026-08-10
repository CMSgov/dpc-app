# frozen_string_literal: true

# Utility methods for views
module ApplicationHelper
  def current_user
    return controller.current_user if controller.respond_to?(:current_user)

    nil # required for lookbook
  end

  def omniauth_authorize_path(service)
    "/auth/#{service}"
  end
end
