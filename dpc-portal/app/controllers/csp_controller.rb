# frozen_string_literal: true

# Base controller for CSP interactions
# rubocop:disable Metrics/ClassLength
class CspController < ApplicationController
  skip_before_action :verify_authenticity_token, only: :openid_connect

  # rubocop:disable Metrics/AbcSize
  def openid_connect
    auth = request.env['omniauth.auth']
    Rails.logger.info 'CHECKING FOR CSP'
    return unless csp

    csp_user = CspUser.find_by(uuid: auth.uid, csp:)
    user = csp_user&.user
    Rails.logger.info "CSP USER FOUND: #{csp_user.to_json}"
    Rails.logger.info "USER FOUND: #{user.to_json}"
    Rails.logger.info "ALL CSP USERS: #{CspUser.all.to_json}"
    Rails.logger.info "ALL USERS: #{User.all.to_json}"

    sign_in_and_log(user)
    ial_2_actions(user, auth)
    update_email(csp_user, user_emails(auth))
    redirect_to path(user, auth)
  end
  # rubocop:enable Metrics/AbcSize

  def no_account
    render(Page::Utility::ErrorComponent.new(nil, 'no_account'), status: :forbidden)
  end

  def failure
    invitation_flow_match = session[:user_return_to]&.match(%r{/organizations/([0-9]+)/invitations/([0-9]+)})
    if invitation_flow_match
      handle_invitation_flow_failure(invitation_flow_match[2])
    elsif params[:code]
      logger.error "#{display_name} Configuration error"
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

    redirect_to logout_url, allow_other_host: true
  end

  private

  # TODO: can this be put in the individual controllers?
  def logout_url
    case csp
    when :id_me then url_for_id_me_logout
    when :login_dot_gov then url_for_login_dot_gov_logout
    else Rails.logger.info(["User attempted to logout with #{display_name} but no active CSP found",
                            { actionContext: LoggingConstants::ActionContext::Authentication,
                              actionType: LoggingConstants::ActionType::InvalidCsp }])
    end
  end

  def url_for_id_me_logout
    state = SecureRandom.hex(16)
    session['omniauth.state'] = state
    client_options = ID_ME_CLIENT_CONFIG[:client_options]
    URI::HTTPS.build(host: client_options[:host],
                     path: client_options[:log_out_path],
                     query: { client_id: client_options[:identifier],
                              redirect_uri: "#{root_url}oauth/logged_out" }.to_query)
  end

  def url_for_login_dot_gov_logout
    state = SecureRandom.hex(16)
    session['omniauth.state'] = state
    client_options = LOGIN_DOT_GOV_CLIENT_CONFIG[:client_options]
    URI::HTTPS.build(host: client_options[:host],
                     path: client_options[:log_out_path],
                     query: { client_id: client_options[:identifier],
                              post_logout_redirect_uri: "#{root_url}auth/logged_out",
                              state: }.to_query)
  end

  def user_emails(auth)
    auth.extra.raw_info.all_emails
  end

  def csp
    Csp.active.find_by(name:).tap do |csp|
      next if csp

      Rails.logger.info(["User attempted to login with #{display_name} but no active CSP found",
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::InvalidCsp }])
      render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail'))
    end
  end

  def sign_in_and_log(user)
    return unless user

    sign_in(user)
    session[:logged_in_at] = Time.now
    Rails.logger.info(['User logged in',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserLoggedIn }])
  end

  def path(user, auth)
    if user.blank? && ial_1?(auth)

      Rails.logger.info(['User logged in without account',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::UserLoginWithoutAccount }])
      return no_account_url
    end
    session.delete(:user_return_to) || organizations_path
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

    # Scan through all of the email from the CSP and add or update as necessary.
    ActiveRecord::Base.transaction do
      add_or_activate_new_email(csp_user, new_emails, existing_emails)
      deactivate_old_email(new_emails, existing_emails)
    end
  end

  def add_or_activate_new_email(csp_user, new_emails, existing_emails)
    new_emails&.each do |new_email|
      existing_email = existing_emails.find { |e| e.email == new_email }
      existing_email ? activate_email(existing_email) : UserEmail.create!(csp_user:, email: new_email, active: true)
    end
  end

  def deactivate_old_email(new_emails, existing_emails)
    # If an existing email is no longer in the list provided by the CSP, deactivate it.
    existing_emails
      &.reject { |e| new_emails&.include?(e.email) }
      &.each { |e| e.update!(active: false, deactivated_at: Time.current, reactivated_at: nil) }
  end

  def activate_email(user_email)
    return if user_email.active

    user_email.update!(active: true, deactivated_at: nil, reactivated_at: Time.current)
  end

  # CSP-specific settings

  # def not_implemented(method) = raise NotImplementedError, "#{self.class} has not implemented method '#{method}'"
  # def name          = not_implemented('name')
  # def display_name  = not_implemented('display_name')
  # def logout_url    = not_implemented('logout_url')
  # def ial_1?(_auth) = not_implemented('ial_1?')
end
# rubocop:enable Metrics/ClassLength
