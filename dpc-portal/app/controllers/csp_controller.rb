# frozen_string_literal: true

# Base controller to handle interactions with CSPs.
# rubocop:disable Metrics/ClassLength
class CspController < ApplicationController
  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']
    return unless (active_csp = csp(auth.provider))

    csp_user = CspUser.find_by(uuid: auth.uid, csp: active_csp)
    user = csp_user&.user
    sign_in_and_log(user, auth.provider)
    ial_2_actions(user, auth)
    update_email(csp_user, user_emails(auth))
    redirect_to path(user, auth)
  end

  def no_account
    render(Page::Utility::ErrorComponent.new(nil, 'no_account'), status: :forbidden)
  end

  def failure
    invitation_flow_match = session[:user_return_to]&.match(%r{/organizations/([0-9]+)/invitations/([0-9]+)})
    if invitation_flow_match
      handle_invitation_flow_failure(invitation_flow_match[2])
    elsif params[:code]
      logger.error 'CSP Configuration error'
      render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail'))
    else
      Rails.logger.info(['User cancelled login',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserCancelledLogin }])
      render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_cancel'))
    end
  end

  def logout
    if params[:invitation_id].present?
      invitation = Invitation.find(params[:invitation_id])
      session[:user_return_to] = organization_invitation_url(invitation.provider_organization.id, invitation.id)
    end

    redirect_to url_for_logout(session[:csp]), allow_other_host: true
  end

  private

  def sign_in_and_log(user, csp)
    return unless user

    sign_in(user:, csp:)
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

  def user_emails(auth)
    auth.extra.raw_info.all_emails
  end

  def maybe_update_user(user, data)
    user&.update(given_name: data.given_name, family_name: data.family_name)
  end

  def update_email(csp_user, new_emails)
    return unless csp_user

    existing_emails = csp_user.user_emails

    # Scan through all of the email from the CSP and add or update as necessary.
    ActiveRecord::Base.transaction do
      add_or_activate_new_email(csp_user, new_emails, existing_emails)
      deactivate_old_email(new_emails, existing_emails)
    end
  end

  def add_or_activate_new_email(csp_user, new_emails, existing_emails)
    new_emails&.each do |new_email|
      existing_email = existing_emails.find do |existing_email|
        existing_email.email == new_email
      end

      if existing_email.nil?
        # Add this email
        UserEmail.create!(csp_user:, email: new_email, active: true)
      else
        # Potentially activate this email
        activate_email(existing_email)
      end
    end
  end

  def deactivate_old_email(new_emails, existing_emails)
    # If an existing email is no longer in the list provided by the CSP, deactivate it.
    existing_emails&.each do |existing_email|
      next if new_emails.include?(existing_email.email)
      existing_email.update!(active: false, deactivated_at: Time.current, reactivated_at: nil)
    end
  end

  def activate_email(user_email)
    return if user_email.active?

    user_email.update!(active: true, deactivated_at: nil, reactivated_at: Time.current)
  end

  def ial_2_actions(user, auth)
    return if ial_1_user?(auth)

    update_csp_tokens(auth)
    maybe_update_user(user, auth.extra.raw_info)
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

    Rails.logger.info(['User attempted to login but no active CSP found',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::InvalidCsp }])
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail'))
    nil
  end

  def update_csp_tokens(auth)
    session[:csp] = auth.provider
    session["#{auth.provider}_token"] = auth.credentials.token
    session["#{auth.provider}_token_exp"] = auth.credentials.expires_in.seconds.from_now
  end
end

protected

# Abstract methods for specific CSPs
def not_implemented(method) = raise NotImplementedError, "Method not implemented: #{method}"
def name = not_implemented('name')
def display_name = not_implemented('display_name')
def ial_1_user?(_auth) = true
# rubocop:enable Metrics/ClassLength
