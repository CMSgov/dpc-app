# frozen_string_literal: true

# Handles interactions with login.gov
class LoginDotGovController < Devise::OmniauthCallbacksController
  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']

    user = User.find_by(provider: auth.provider, uid: auth.uid)
    if user
      sign_in(:user, user)
      session[:logged_in_at] = Time.now
    end
    ial_2_actions(user, auth)
    redirect_to path(user, auth)
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

  def logged_out
    redirect_to session.delete(:user_return_to) || organizations_path
  end

  private

  def maybe_update_user(user, data)
    user&.update(given_name: data.given_name, family_name: data.family_name)
  end

  def ial_2_actions(user, auth)
    data = auth.extra.raw_info

    return unless data.ial == 'http://idmanagement.gov/ns/assurance/ial/2'

    maybe_update_user(user, data)
    session[:login_dot_gov_token] = auth.credentials.token
    session[:login_dot_gov_token_exp] = auth.credentials.expires_in.seconds.from_now
  end

  def path(user, auth)
    if user.blank? && auth.extra.raw_info.ial == 'http://idmanagement.gov/ns/assurance/ial/1'
      flash[:alert] = 'You must have an account to sign in.'
      return new_user_session_url
    end
    session.delete(:user_return_to) || organizations_path
  end
end
