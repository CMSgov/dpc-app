# frozen_string_literal: true

# Base controller to handle interactions with CSPs.
class CspController < ApplicationController # rubocop:disable Metrics/ClassLength
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
    render(Page::Utility::ErrorComponent.new(nil, 'no_account', csp: csp_session.current), status: :forbidden)
  end

  def failure
    csp = csp_session.current
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

    redirect_to url_for_logout(csp_session.current), allow_other_host: true
  end

  private

  def user_actions(auth, csp)
    csp_user = CspUser.find_by(uuid: auth.uid, csp:)
    user = csp_user&.user

    sign_in_and_log(user, csp.name)
    ial_2_actions(user, auth)
    handle_csp_user_response(csp_user, auth)
  end

  def handle_csp_user_response(csp_user, auth)
    return render_add_email(csp_user, auth) if csp_user && !email_match?(csp_user, auth)

    orig_csp_user = existing_account(auth)
    return render_link_account(primary_email(auth), orig_csp_user.csp) if orig_csp_user.present?

    # check for other active CSPs, fetch userinfo for each and verify all SSNs match, create CspUser for current_user
    check_csp_session(orig_csp_user, auth.uid)

    sync_and_redirect(csp_user, auth)
  end

  def sync_and_redirect(csp_user, auth)
    sync_csp_emails(csp_user, all_emails(auth), primary_email(auth))
    redirect_to path(csp_user&.user, auth)
  end

  def email_match?(csp_user, auth)
    csp_user.user_emails.empty? || csp_user.user_emails.map(&:email).include?(primary_email(auth))
  end

  def name_match?(user, auth)
    info = auth.extra.raw_info
    user.given_name.casecmp?(info.given_name) && user.family_name.casecmp?(info.family_name)
  end

  def existing_account(auth)
    email = UserEmail.includes(csp_user: :user).find_by(email: primary_email(auth))
    return nil unless email

    csp_user = email.csp_user
    return nil unless csp_user.csp == auth.provider.to_sym

    csp_user if name_match?(csp_user.user, auth)
  end

  def render_add_email(csp_user, auth)
    Rails.logger.info(['User has existing account associated with different email',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::MergeUserAccountEmail,
                         **csp_log_context }])
    render(Page::ExistingAccount::AddEmailComponent.new(csp_user.user.email, csp_user.csp.name,
                                                        update_path(id: csp_user.id,
                                                                    csp: csp_user.csp.id,
                                                                    all_emails: all_emails(auth),
                                                                    primary_email: primary_email(auth))))
  end

  def render_link_account(email, csp)
    Rails.logger.info(['User has existing account associated with different CSP',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::MergeUserAccountCsp,
                         **csp_log_context }])
    render(Page::ExistingAccount::LinkAccountComponent.new(email, csp))
  end

  def render_ial1_blocked
    Rails.logger.info(["User attempted IAL1 login with #{display_name || 'CSP'} — not permitted",
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }])
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: csp_code), status: :forbidden)
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

  def check_csp_session(user, uid)
    return if csp_session.active_csps.one?

    verify_account_match
    create_csp_user(user, uid)
  end

  def verify_account_match
    all_user_info = UserInfoService.new.all_user_info(csp_session)
    all_ssns = all_user_info.values.map { |user_info| ssn(user_info) }
    raise 'SSN mismatch' unless all_ssns.uniq.one?
  end

  def ssn(user_info)
    user_info['social_security_number'] || user_info['ssn']
  end

  def create_csp_user(user, uuid)
    CspUser.find_or_create_by!(user:, csp: csp_session.current, uuid:)
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
  def store_id_token? = false
end
