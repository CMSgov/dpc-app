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
      Rails.logger.info(['User logged in',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoggedIn }])
    end
    ial_2_actions(user, auth)
    redirect_to path(user, auth)
  end

  def failure
    invitation_flow_match = session[:user_return_to]&.match(%r{/organizations/([0-9]+)/invitations/([0-9]+)})
    if invitation_flow_match
      handle_invitation_flow_failure(invitation_flow_match[2])
    elsif params[:code]
      @message = 'Something went wrong.'
      logger.error 'Login.gov Configuration error'
    else
      @message = 'You have decided not to authenticate via login.gov.'
      Rails.logger.info(['User cancelled login',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserCancelledLogin }])
    end
  end

  # Documentation at https://developers.login.gov/oidc/logout/
  def logout
    if params[:invitation_id].present?

      invitation = Invitation.find(params[:invitation_id])
      session[:user_return_to] = organization_invitation_url(invitation.provider_organization.id, invitation.id)
    end
    redirect_to url_for_login_dot_gov_logout, allow_other_host: true
  end

  # Return from login.gov
  def logged_out
    redirect_to session.delete(:user_return_to) || new_user_session_path
  end

  private

  def handle_invitation_flow_failure(invitation_id)
    Rails.logger.info(['Failed invitation flow',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::FailedLogin }])
    invitation = Invitation.find(invitation_id)
    if invitation.credential_delegate?
      render(Page::Invitations::BadInvitationComponent.new(invitation, 'fail_to_proof'),
             status: :forbidden)
    else
      render(Page::Invitations::AoFlowFailComponent.new(invitation, 'fail_to_proof', 1),
             status: :forbidden)
    end
  end

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
      Rails.logger.info(['User logged in without account',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }])
      return new_user_session_url
    end
    session.delete(:user_return_to) || organizations_path
  end
end
