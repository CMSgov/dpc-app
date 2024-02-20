# frozen_string_literal: true

# Handles interactions with login.gov
class LoginDotGovController < Devise::OmniauthCallbacksController
  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']
    user = User.find_or_create_by(provider: auth.provider, uid: auth.uid) do |user_to_create|
      assign_user_properties(user_to_create, auth)
    end
    ssn_cookie(auth.extra.raw_info.social_security_number)
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

  private

  def assign_user_properties(user, auth)
    user.email = auth.info.email
    user.given_name = auth.extra.raw_info.given_name
    user.family_name = auth.extra.raw_info.family_name
    user.password = user.password_confirmation = Devise.friendly_token[0, 20]
  end

  def ssn_cookie(ssn)
    # We will be comparing against ssn in PECOS, which does not contain dashes
    hex = Digest::SHA2.new(256).hexdigest(ssn.tr('^0-9', ''))
    cookies.signed.encrypted[:ao_id] = {
      value: hex,
      http_only: true,
      expires: 30.minutes
    }
  end
end
