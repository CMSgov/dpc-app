module InternalUserDeviseHelper
  def after_sign_in_path_for(resource)
    internal_users_path
  end
end
