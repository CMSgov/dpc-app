# frozen_string_literal: true

# Handles interactions with login.gov
class LoginDotGovController < Devise::OmniauthCallbacksController
  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']
    user = User.new(email: auth.info.email,
                    given_name: auth.extra.raw_info.given_name,
                    family_name: auth.extra.raw_info.family_name)
    sign_in(:user, user)
  end

  def failure
    if params[:code]
      @message = 'Something went wrong.'
      logger.error 'Login.gov Configuration error'
    else
      @message = 'You have decided not to authenticate via login.gov.'
      logger.warn 'User decided not to continue logging in'
    end
  end
end
