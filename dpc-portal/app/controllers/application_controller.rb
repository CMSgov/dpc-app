# frozen_string_literal: true

# Parent class of all controllers
class ApplicationController < ActionController::Base
  before_action :check_session_length
  before_action :set_current_request_attributes
  before_action :no_store

  auto_session_timeout User.timeout_in

  def active_url
    '/active'
  end

  def current_user
    @current_user ||= User.where(id: session['user']).first
  end

  def authenticate_user!
    return if current_user

    flash[:alert] = t('devise.failure.unauthenticated')
    session[:user_return_to] = request.path
    redirect_to sign_in_path
  end

  def sign_in(user, csp: :login_dot_gov)
    session['user'] = user.id
    session[:csp] = csp
  end

  private

  def check_user_verification
    # puts current_user.inspect
    # puts "Current user verification status: #{current_user.verification_status}" if current_user
    return unless current_user&.rejected?

    render(Page::Utility::AccessDeniedComponent.new(failure_code: "verification.#{current_user.verification_reason}"))
  end

  def tos_accepted
    return if @organization.terms_of_service_accepted_by.present?

    if current_user.ao?(@organization)
      render(Page::Organization::TosFormComponent.new(@organization))
    else
      flash[:notice] = 'Organization is not ready for credential management'
      redirect_to organizations_path
    end
  end

  def url_for_logout(csp)
    case csp.to_s
    when :id_me.to_s
      url_for_id_me_logout
    when :login_dot_gov.to_s
      url_for_login_dot_gov_logout
    when :clear.to_s
      url_for_clear_logout
    else
      raise "Unsupported CSP: #{csp}"
    end
  end

  # Documentation at https://developers.login.gov/oidc/logout/
  def url_for_login_dot_gov_logout
    state = SecureRandom.hex(16)
    session['omniauth.state'] = state
    csp_config = CspConfig.for(:login_dot_gov)
    URI::HTTPS.build(host: csp_config.host,
                     path: csp_config.log_out_path,
                     query: { client_id: csp_config.identifier,
                              post_logout_redirect_uri: "#{root_url}auth/logged_out",
                              state: }.to_query)
  end

  def url_for_id_me_logout
    state = SecureRandom.hex(16)
    session['omniauth.state'] = state
    URI::HTTPS.build(host: CspConfig.for(:id_me).host,
                     path: CspConfig.for(:id_me).log_out_path,
                     query: { client_id: CspConfig.for(:id_me).identifier,
                              redirect_uri: "#{root_url}auth/logged_out" }.to_query)
  end

  def url_for_clear_logout
    state = SecureRandom.hex(16)
    session['omniauth.state'] = state
    csp_config = CspConfig.for(:clear)
    URI::HTTPS.build(host: csp_config.host,
                     path: csp_config.log_out_path,
                     query: { client_id: csp_config.identifier,
                              post_logout_redirect_uri: "#{root_url}auth/logged_out",
                              id_token_hint: session['clear_id_token']
                              }.to_query)
  end

  # rubocop:disable Metrics/AbcSize
  def check_session_length
    session[:logged_in_at] = Time.now if session[:logged_in_at].nil?
    max_session = User.remember_for.to_i / 60
    return unless max_session.minutes.ago > session[:logged_in_at]

    reset_session
    flash[:notice] = t('devise.failure.max_session_timeout', default: 'Your session has timed out.')
    Rails.logger.info(['User session timed out',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::SessionTimedOut }])
    redirect_to sign_in_path
  end
  # rubocop:enable Metrics/AbcSize

  def organization_id
    params[:organization_id]
  end

  def load_organization
    @organization = ProviderOrganization.find(organization_id)
    CurrentAttributes.save_organization_attributes(@organization, current_user)
  rescue ActiveRecord::RecordNotFound
    render file: "#{Rails.root}/public/404.html", layout: false, status: :not_found
  end

  def require_can_access
    redirect_to organizations_path unless current_user.can_access?(@organization)

    verify_status
  end

  def require_ao
    redirect_to organizations_path unless current_user.ao?(@organization)

    verify_status
  end

  def verify_status
    if @organization.rejected?
      failure_code = "#{code_prefix}.#{@organization.verification_reason}"
      return render(Page::Utility::AccessDeniedComponent.new(organization: @organization, failure_code:))
    end

    links = current_user.ao_org_links.where(provider_organization: @organization)
    return if links.empty? || links.any?(&:verification_status?)

    failure_code = "verification.#{links.first.verification_reason}"
    render(Page::Utility::AccessDeniedComponent.new(organization: @organization, failure_code:))
  end

  def code_prefix
    has_ao_link = current_user.ao_org_links.where(provider_organization: @organization).exists?
    has_ao_link ? 'verification' : 'cd_access'
  end

  def set_current_request_attributes
    ::NewRelic::Agent.add_custom_attributes({ user_id: current_user.id }) if current_user
    CurrentAttributes.save_request_attributes(request)
    CurrentAttributes.save_user_attributes(current_user)
  end

  def log_credential_action(credential_type, dpc_api_credential_id, action)
    log = CredentialAuditLog.new(user: current_user,
                                 credential_type:,
                                 dpc_api_credential_id:,
                                 action:)
    return if log.save

    logger.error(['CredentialAuditLog failure', { action:, credential_type:, dpc_api_credential_id: }])
  end
end
