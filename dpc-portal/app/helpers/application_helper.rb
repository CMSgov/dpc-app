# frozen_string_literal: true

# Utility methods for views
module ApplicationHelper
  def omniauth_authorize_path(service)
    "/auth/#{service}"
  end
end
