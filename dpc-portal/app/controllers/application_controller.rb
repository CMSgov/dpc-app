# frozen_string_literal: true

# Parent class of all controllers
class ApplicationController < ActionController::Base
  include CspLogout
  include OrganizationAccess

  before_action :check_session_length
  before_action :set_current_request_attributes
  before_action :no_store

  auto_session_timeout User.timeout_in

  def active_url
    '/active'
  end

  def current_user
    @current_user ||= User.find_by(id: session[:user])
  end

  def authenticate_user!
    return if current_user

    flash[:alert] = t('devise.failure.unauthenticated')
    session[:user_return_to] = request.path
    redirect_to sign_in_path
  end

  def sign_in(user:, csp:)
    session[:user] = user.id
    session[:csp] = csp.to_s
  end

  private

  def check_user_verification
    return unless current_user&.rejected?

    render(Page::Utility::AccessDeniedComponent.new(failure_code: "verification.#{current_user.verification_reason}"))
  end

  def check_session_length
    session[:logged_in_at] ||= Time.now
    return unless session_timed_out?

    reset_session
    flash[:notice] = t('devise.failure.max_session_timeout', default: 'Your session has timed out.')
    Rails.logger.info(['User session timed out',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::SessionTimedOut }])
    redirect_to sign_in_path
  end

  def session_timed_out?
    User.remember_for.ago > session[:logged_in_at]
  end

  def set_current_request_attributes
    # I hate naming this "usr.id", but that's DataDog's standard
    Datadog::Tracing.active_span&.set_tag('usr.id', current_user.id) if current_user

    CurrentAttributes.save_request_attributes(request)
    CurrentAttributes.save_user_attributes(current_user, csp_name: session[:csp])
  end

  def log_credential_action(credential_type, dpc_api_credential_id, action)
    log = CredentialAuditLog.new(user: current_user,
                                 credential_type:,
                                 dpc_api_credential_id:,
                                 action:)
    return if log.save

    logger.error(['CredentialAuditLog failure', { action:, credential_type:, dpc_api_credential_id: }])
  end

  # Helper method for logging csp with actionContext and actionType whenever it's available on the session
  def csp_log_context
    session[:csp].present? ? { csp: session[:csp] } : {}
  end
end
