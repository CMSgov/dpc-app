# frozen_string_literal: true

# Handles interactions with login.gov.
# This class is > 100 lines and my attempts to refactor made it uglier and too complex to pass the ABC
# check, so I disabled the class length check.  When we create controllers for the other CSPs we can pull
# out common code and turn the check back on.

# rubocop:disable Metrics/ClassLength
class LoginDotGovController < ApplicationController
  skip_before_action :verify_authenticity_token, only: :openid_connect

  def openid_connect
    auth = request.env['omniauth.auth']
    return unless (csp = csp())

    csp_user = CspUser.find_by(uuid: auth.uid, csp:)

    user = csp_user&.user
    sign_in_and_log(user)
    post_signin_actions(user, csp_user, auth)
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
      logger.error 'Login.gov Configuration error'
      render(Page::Utility::ErrorComponent.new(nil, 'login_gov_signin_fail'))
    else
      Rails.logger.info(['User cancelled login',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserCancelledLogin }])
      render(Page::Utility::ErrorComponent.new(nil, 'login_gov_signin_cancel'))
    end
  end

  def logout
    if params[:invitation_id].present?
      invitation = Invitation.find(params[:invitation_id])
      session[:user_return_to] = organization_invitation_url(invitation.provider_organization.id, invitation.id)
    end

    redirect_to url_for_login_dot_gov_logout, allow_other_host: true
  end

  private

  def sign_in_and_log(user)
    return unless user

    sign_in(user)
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

  def maybe_update_user(user, data)
    user&.update(given_name: data.given_name, family_name: data.family_name)
  end

  def update_email(csp_user, new_emails)
    return unless csp_user

    existing_emails = csp_user.user_emails

    # Scan through all of the email from the CSP and add or update as necesssary.
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
      unless new_emails&.include?(existing_email.email)
        existing_email.update!(active: false, deactivated_at: Time.current, reactivated_at: nil)
      end
    end
  end

  def activate_email(user_email)
    return unless user_email.active == false

    user_email.update!(active: true, deactivated_at: nil, reactivated_at: Time.current)
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
      Rails.logger.info(['User logged in without account',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }])
      return no_account_url
    end
    session.delete(:user_return_to) || organizations_path
  end

  def csp
    csp = Csp.active.find_by(name: :login_dot_gov)
    return csp if csp

    Rails.logger.info(['User attempted to login with Login.gov but no active CSP found',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::InvalidCsp }])
    render(Page::Utility::ErrorComponent.new(nil, 'login_gov_signin_fail'))
    nil
  end

  def post_signin_actions(user, csp_user, auth)
    ial_2_actions(user, auth)
    update_email(csp_user, auth.extra.raw_info.all_emails)
  end
end
# rubocop:enable Metrics/ClassLength
