# frozen_string_literal: true

# Base controller to handle interactions with CSPs.
class CspController < ApplicationController
  include CspEmailSync

  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']
    return unless (active_csp = csp(auth.provider))

    user_actions(auth, active_csp)
  end

  def no_account
    render(Page::Utility::ErrorComponent.new(nil, 'no_account', csp: session[:csp]), status: :forbidden)
  end

  def failure
    csp = session[:csp]
    invitation_flow_match = session[:user_return_to]&.match(%r{/organizations/([0-9]+)/invitations/([0-9]+)})
    return handle_invitation_flow_failure(invitation_flow_match[2]) if invitation_flow_match
    return handle_signin_fail(csp) if params[:code]

    handle_signin_cancel(csp)
  end

  def logout
    if params[:invitation_id].present?
      invitation = Invitation.find(params[:invitation_id])
      session[:user_return_to] = organization_invitation_url(invitation.provider_organization.id, invitation.id)
    end

    redirect_to url_for_logout(session[:csp]), allow_other_host: true
  end

  private

  def user_actions(auth, csp)
    csp_user = CspUser.find_by(uuid: auth.uid, csp:)
    user = csp_user&.user
    sign_in_and_log(user, auth.provider)
    ial_2_actions(user, auth)
    sync_csp_emails(csp_user, all_emails(auth), primary_email(auth))
    redirect_to path(user, auth)
  end

  def sign_in_and_log(user, csp)
    return unless user

    sign_in(user:, csp:)
    cookies.permanent[:last_used_csp] = csp
    session[:logged_in_at] = Time.now
    Rails.logger.info(['User logged in',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserLoggedIn }])
  end

  def handle_invitation_flow_failure(invitation_id)
    Rails.logger.info(['Failed invitation flow',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::FailedLogin }])
    invitation = Invitation.find(invitation_id)
    if invitation.credential_delegate?
      render(Page::Utility::ErrorComponent.new(invitation, 'fail_to_proof'), status: :forbidden)
    else
      render(Page::Invitations::AoFlowFailComponent.new(invitation, 'fail_to_proof', 1), status: :forbidden)
    end
  end

  def handle_signin_fail(csp)
    logger.error 'CSP Configuration error'
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp:))
  end

  def handle_signin_cancel(csp)
    Rails.logger.info(['User cancelled login',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserCancelledLogin }])
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_cancel', csp:))
  end

  def ial_2_actions(user, auth)
    return if ial_1_user?(auth)

    update_csp_tokens(auth)
    user&.update(given_name: auth.extra.raw_info.given_name, family_name: auth.extra.raw_info.family_name)
  end

  def path(user, auth)
    if user.blank? && ial_1_user?(auth)

      Rails.logger.info(['User logged in without account',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }])
      return no_account_url
    end
    session.delete(:user_return_to) || organizations_path
  end

  def csp(name)
    active_csp = Csp.active.find_by(name:)
    return active_csp if active_csp

    Rails.logger.info(["User attempted to login with #{display_name} but no active CSP found",
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::InvalidCsp }])
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: name.to_sym))
    nil
  end

  def update_csp_tokens(auth)
    session[:csp] = auth.provider
    session["#{auth.provider}_token"] = auth.credentials.token
    session["#{auth.provider}_token_exp"] = auth.credentials.expires_in.seconds.from_now
  end

  # Can be overridden
  def primary_email(auth) = auth.info.email
  def all_emails(auth) = auth.extra.raw_info.all_emails

  # Must be implemented
  def csp_code = not_implemented('csp_code')
  def display_name = not_implemented('display_name')
  def ial_1_user?(_auth) = not_implemented('ial_1_user?')
  def not_implemented(method) = raise NotImplementedError, "Method not implemented: #{method}"
end
