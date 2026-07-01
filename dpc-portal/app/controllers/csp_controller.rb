# frozen_string_literal: true

# Base controller to handle interactions with CSPs.
class CspController < ApplicationController
  include CspEmailSync
  include CspErrorHandling

  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']
    return render_ial1_blocked if ial_1_user?(auth)

    return unless (active_csp = csp(auth.provider))

    user_actions(auth, active_csp)
  end

  def no_account
    render(Page::Utility::ErrorComponent.new(nil, 'no_account', session[:csp]), status: :forbidden)
  end

  def failure
    csp = session[:csp]
    invitation_flow_match = session[:user_return_to]&.match(%r{/organizations/([0-9]+)/invitations/([0-9]+)})
    return handle_invitation_flow_failure(invitation_flow_match[2], csp) if invitation_flow_match
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
    sign_in_and_log(user, csp.name)
    sync_csp_emails(csp_user, all_emails(auth), primary_email(auth))
    ial_2_actions(user, auth)
    redirect_to path(user, auth)
  end

  def render_ial1_blocked
    Rails.logger.info(["User attempted IAL1 login with #{display_name || 'CSP'} — not permitted",
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }])
    render(Page::Utility::ErrorComponent.new(nil, "csp_signin_fail", csp_code), status: :forbidden)
  end

  def sign_in_and_log(user, csp)
    return unless user

    sign_in(user:, csp:)
    session[:logged_in_at] = Time.now
    cookies.permanent[:last_used_csp] = csp
    Rails.logger.info(['User logged in',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserLoggedIn,
                         **csp_log_context }])
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
                           actionType: LoggingConstants::ActionType::UserLoginWithoutAccount,
                           **csp_log_context }])
      return no_account_url
    end
    session.delete(:user_return_to) || organizations_path
  end

  def csp(name)
    active_csp = Csp.active.find_by(name:)
    return active_csp if active_csp

    Rails.logger.info(["User attempted to login with #{display_name || name} but no active CSP found",
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::InvalidCsp,
                         **csp_log_context }])
    render(Page::Utility::ErrorComponent.new(nil, "csp_signin_fail", csp_code))
    nil
  end

  def update_csp_tokens(auth)
    session[:csp] = auth.provider.to_s
    session["#{auth.provider}_token"] = auth.credentials.token
    session["#{auth.provider}_token_exp"] = auth.credentials.expires_in.seconds.from_now
  end

  # Can be overridden
  def primary_email(auth) = auth.info.email
  def all_emails(auth) = auth.extra.raw_info.all_emails

  def ial_1_user?(auth) = auth.extra.raw_info.ial == 'http://idmanagement.gov/ns/assurance/ial/1'
end
