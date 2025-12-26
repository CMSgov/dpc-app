module ApplicationHelper
  def current_user
    @current_user
  end

  def omniauth_authorize_path(service)
    return "/portal/auth/#{service}"
  end
end
