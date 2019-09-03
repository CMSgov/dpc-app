# frozen_string_literal: true

module InternalUserDeviseHelper
  def after_sign_in_path_for(*)
    internal_users_path
  end
end
