# frozen_string_literal: true

# Base controller to handle interactions with CSPs.
class CspController < ApplicationController
  include CspEmailSync
  include CspErrorHandling
  include CspExistingAccount

  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    return render_ial1_blocked if ial_1_user?(auth_details)
    return unless (active_csp = csp(auth_details.provider))

    if sign_in_canceled?(auth_details)
      return redirect_to csp_failure_path(message: 'access_denied', strategy: active_csp.name)
    end

    user_actions(auth_details, active_csp)
  end

  def no_account
    render(Page::Utility::ErrorComponent.new(nil, 'no_account', csp: csp_session.current), status: :forbidden)
  end

  def failure
    # send invitation failures back to the invitation with an alert, rather than an error
    invitation_flow_match = session[:user_return_to]&.match(%r{/organizations/([0-9]+)/invitations/([0-9]+)})

    if invitation_flow_match
      return handle_invitation_flow_failure(session[:user_return_to], invitation_flow_match[2])
    end

    return handle_csp_auth_error if csp_auth_error?
    return handle_signin_cancel if csp_user_cancelled?

    handle_signin_fail
  end

  def logout
    if params[:invitation_id].present?
      invitation = Invitation.find(params[:invitation_id])
      session[:user_return_to] = organization_invitation_url(invitation.provider_organization.id, invitation.id)
    end

    redirect_to url_for_logout(csp_session.current), allow_other_host: true
  end

  private

  def auth_details
    @auth_details ||= request.env['omniauth.auth']
  end

  def user_actions(auth, csp)
    csp_user = find_csp_user(csp)
    user = csp_user&.user

    sign_in_and_log(user, csp.name)
    ial_2_actions(user, auth)
    handle_csp_user_response(csp_user, auth)
  end

  def sync_and_redirect(csp_user, auth)
    sync_csp_emails(csp_user, all_emails(auth), primary_email(auth))
    redirect_to path(csp_user&.user, auth)
  end

  def render_ial1_blocked
    log_event(:info, "User attempted IAL1 login with #{display_name || 'CSP'} — not permitted",
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserLoginWithoutAccount,
              user_identifier: @auth_details&.uid,
              csp: @auth_details&.provider)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: csp_code), status: :forbidden)
  end

  def sign_in_and_log(user, csp)
    return unless user

    sign_in(user:, csp:)
    session[:logged_in_at] = Time.now
    cookies.permanent[:last_used_csp] = csp
    log_event(:info, 'User logged in',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserLoggedIn,
              user_identifier: @auth_details&.uid,
              csp: @auth_details&.provider)
  end

  def ial_2_actions(user, auth)
    return if ial_1_user?(auth)

    update_csp_tokens(auth)
    user&.update(given_name: auth.extra.raw_info.given_name, family_name: auth.extra.raw_info.family_name)
  end

  def path(user, auth)
    if user.blank? && ial_1_user?(auth)
      log_event(:info, 'User logged in without account',
                action_context: LoggingConstants::ActionContext::Authentication,
                action_type: LoggingConstants::ActionType::UserLoginWithoutAccount,
                user_identifier: auth.uid,
                csp: auth.provider)
      return no_account_url
    end
    session.delete(:user_return_to) || organizations_path
  end

  def csp(name)
    active_csp = Csp.active.find_by(name:)
    return active_csp if active_csp

    log_event(:info, "User attempted to login with #{display_name || name} but no active CSP found",
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::InvalidCsp,
              user_identifier: @auth_details&.uid,
              csp: @auth_details&.provider)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: csp_code))
    nil
  end

  def update_csp_tokens(auth)
    csp_session.store(csp: auth.provider,
                      token: auth.credentials.token,
                      token_exp: auth.credentials.expires_in.seconds.from_now,
                      id_token: store_id_token? ? auth.credentials.id_token : nil)
  end

  # Can be overridden
  def primary_email(auth) = auth.info.email
  def all_emails(auth) = auth.extra.raw_info.all_emails

  def ial_1_user?(auth) = auth.extra.raw_info.ial == 'http://idmanagement.gov/ns/assurance/ial/1'
  def sign_in_canceled?(auth) = false
  def store_id_token? = false
end
