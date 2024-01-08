# frozen_string_literal: true

# Handles interactions with login.gov
class OmniauthCallbacksController < Devise::OmniauthCallbacksController
  skip_before_action :verify_authenticity_token, only: :openid_connect
  # You should configure your model like this:
  # devise :omniauthable, omniauth_providers: [:twitter]

  def openid_connect
    auth = request.env['omniauth.auth']
    logger.info(auth.provider)
    logger.info(auth.uid)
    logger.info(auth.extra.raw_info)
    render plain: auth.extra.raw_info
  end

  # protected

  # The path used when OmniAuth fails
  # def after_omniauth_failure_path_for(scope)
  #   super(scope)
  # end
end
