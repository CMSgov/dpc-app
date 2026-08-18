# frozen_string_literal: true

require 'dpc_portal_utils'

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  include CspEmailSync
  include CspUtils
  include DpcPortalUtils

  before_action :load_organization
  before_action :load_invitation
  before_action :validate_invitation, except: %i[renew]
  before_action :verify_ao_invitation, only: %i[accept confirm]
  before_action :verify_cd_invitation, only: %i[code verify_code confirm_cd]
  before_action :check_for_token, only: %i[accept confirm confirm_cd register]
  before_action :block_test_utilities, only: %i[set_idp_token]

  def show
    log_invitation_flow_start
    render(Page::Invitations::StartComponent.new(@organization, @invitation))
  end

  # AO Flow
  def accept
    invitation_matches_user
    return if performed?

    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation, @given_name, @family_name))
  end

  def confirm
    unless session["invitation_status_#{@invitation.id}"] == 'identity_verified'
      return redirect_to accept_organization_invitation_url(@organization, @invitation)
    end

    verify_user_is_ao
    return if performed?

    session["invitation_status_#{@invitation.id}"] = 'verification_complete'
    render(Page::Invitations::RegisterComponent.new(@organization, @invitation))
  end

  # CD Flow
  def confirm_cd
    invitation_matches_user
    return if performed?

    session["invitation_status_#{@invitation.id}"] = 'verification_complete'
    log_event(:info, 'Approved access authorization occurred for the Credential Delegate',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::CdConfirmed,
              invitation: @invitation.id)
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation, @given_name, @family_name))
  end

  # Everybody
  def register
    unless session["invitation_status_#{@invitation.id}"] == 'verification_complete'
      return redirect_to organization_invitation_url(@organization, @invitation)
    end

    return unless create_link

    complete_registration
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 2)
  end

  def login
    csp_name = params[:provider]
    login_session(csp_name)
    log_event(:info, 'User began login flow',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::BeginLogin,
              invitation: @invitation.id)
    csp_login_actions(csp_name)
  end

  def renew
    if @invitation.renew
      flash[:notice] = 'You should receive your new invitation shortly'
    else
      flash[:alert] = 'Unable to create new invitation'
    end
    redirect_to accept_organization_invitation_url(@organization, @invitation)
  end

  def set_idp_token
    csp_session.store(csp: params[:provider], token: 'token', token_exp: 2.days.from_now)
    head :ok
  end

  private

  def complete_registration
    session.delete("invitation_status_#{@invitation.id}")
    sign_in(user: @user, csp: csp_session.current)
    log_event(:info, 'User logged in',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::UserLoggedIn,
              user_identifier: current_csp_user_identifier,
              invitation: @invitation.id)
    render(Page::Invitations::SuccessComponent.new(@organization, @invitation, @given_name, @family_name))
  end

  def csp_login_actions(csp)
    csp_config = CspConfig.for(csp)
    url = URI(csp_config.authorization_endpoint)
    query = { client_id: csp_config.identifier,
              redirect_uri: "#{my_protocol_host}#{csp_config.redirect_path}",
              response_type: 'code',
              acr_values: csp_config.acr_values.presence,
              scope: csp_config.authorize_scope,
              nonce: @nonce,
              state: @state }
    url.query = query.compact.to_query
    redirect_to url, allow_other_host: true
  end

  def invitation_matches_user
    user_info = UserInfoService.new.user_info(csp_session)
    return if render_bad_invitation?(user_info)

    session["invitation_status_#{@invitation.id}"] = 'identity_verified'
    @given_name = user_info['given_name']
    @family_name = user_info['family_name']
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 1)
  end

  def render_bad_invitation?(user_info)
    csp = csp_session.current
    if @invitation.credential_delegate? && !@invitation.cd_match?(user_info)
      log_pii_mismatch(user_info)
      render(Page::Utility::ErrorComponent.new(@invitation, 'pii_mismatch', csp:),
             status: :forbidden)
    elsif !@invitation.email_match?(user_info) && !confirmed_email?(user_info)
      log_pii_mismatch(user_info)
      render(Page::Utility::ErrorComponent.new(@invitation, 'email_mismatch', csp:),
             status: :forbidden)
    end
  end

  def verify_user_is_ao
    user_info = UserInfoService.new.user_info(csp_session)
    result = @invitation.ao_match?(user_info)
    session[:user_pac_id] = result.dig(:ao_role, 'pacId')
    log_waivers(result)
  rescue VerificationError => e
    status = AoVerificationService::SERVER_ERRORS.include?(e.message) ? :service_unavailable : :forbidden
    log_ao_verification_error(user_info, e, status == :service_unavailable)
    render(Page::Invitations::AoFlowFailComponent.new(@invitation, e.message, 2), status:)
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 2)
  end

  def handle_user_info_service_error(error, step)
    log_event(:error, 'User Info Service unavailable',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::OidcUserInfoFailed,
              invitation: @invitation.id,
              error: error.message)

    if error.message == 'unauthorized'
      render(Page::Invitations::InvitationLoginComponent.new(@invitation))
    elsif @invitation.credential_delegate?
      render(Page::Utility::ErrorComponent.new(@invitation, error.message, csp: csp_session.current),
             status: :service_unavailable)
    else
      render(Page::Invitations::AoFlowFailComponent.new(@invitation, error.message, step),
             status: :service_unavailable)
    end
  end

  def login_session(csp_name)
    session[:user_return_to] = invitation_return_url
    session['omniauth.nonce'] = @nonce = SecureRandom.hex(16)
    session['omniauth.state'] = @state = SecureRandom.hex(16)
    csp_session.activate(csp_name)
  end

  def invitation_return_url
    if @invitation.authorized_official?
      accept_organization_invitation_url(@organization, params[:id])
    else
      confirm_cd_organization_invitation_url(@organization, params[:id])
    end
  end

  def create_link
    if @invitation.credential_delegate?
      create_cd_org_link
    elsif @invitation.authorized_official?
      create_ao_org_link
    else
      invalid_status = @invitation.credential_delegate? ? 'cd_invalid' : 'ao_invalid'
      render(Page::Utility::ErrorComponent.new(@invitation, invalid_status),
             status: :unprocessable_entity)
      false
    end
  rescue MultiUserMatchError => e
    handle_multi_user_match_error(e)
  end

  def handle_multi_user_match_error(error)
    user_info = UserInfoService.new.user_info(csp_session)
    log_event(:error, 'User matches too many existing users',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::MultiUserMatch,
              user_identifier: user_info['sub'],
              csp: csp_session.current,
              invitation: @invitation.id,
              error: error.message)
    render(Page::Utility::ErrorComponent.new(@invitation, 'multi_user_match', csp: csp_session.current))
    nil
  end

  def create_cd_org_link
    CdOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    log_event(:info, 'Credential Delegate linked to organization',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::CdLinkedToOrg,
              organization_npi: @organization.npi,
              invitation: @invitation.id)
    @invitation.accept!
  end

  def create_ao_org_link
    AoOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    log_event(:info, 'Authorized Official linked to organization',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::AoLinkedToOrg,
              organization_npi: @organization.npi,
              invitation: @invitation.id)
    @invitation.accept!
    @user.update(verification_status: 'approved')
    @organization.update(verification_status: 'approved')
  end

  def user
    user_info = UserInfoService.new.user_info(csp_session)
    find_or_create_user(user_info)
    csp = Csp.find_by(name: csp_session.current)
    csp_user = CspUser.find_or_create_by!(user: @user, csp:, uuid: user_info['sub'])

    new_emails = user_emails(user_info)
    primary_email = user_info['email']
    sync_csp_emails(csp_user, new_emails, primary_email)
    update_user(user_info)
    @user
  end

  def user_emails(user_info)
    user_info['all_emails'] || user_info['emails'] || user_info['emails_confirmed'] || [user_info['email']]
  end

  def confirmed_email?(user_info)
    user_emails(user_info).include?(@invitation.invited_email)
  end

  def find_or_create_user(user_info)
    # Unique PacIds only available in prod. This will be revisited in DPC-5566
    @user = if @invitation.authorized_official? && pac_id_available?
              find_or_create_ao_user(user_info)
            else
              find_or_create_new_user(user_info)
            end
  end

  def pac_id_available?
    ENV['ENV'] == 'prod' || Rails.env.test? # CPI API Gateway mocked in tests
  end

  def find_or_create_new_user(user_info)
    find_existing_user(user_info) || create_new_user(user_info)
  end

  def find_existing_user(user_info)
    find_user_by_uuid(user_info) ||
      find_user_by_email(user_info)
  end

  def find_user_by_uuid(user_info)
    User.find_by_csp_uid(name: csp_session.current, csp_uid: user_info['sub'])
  end

  def find_or_create_ao_user(user_info)
    candidates = find_ao_candidates(user_info)

    if candidates.size > 1
      log_event(:error, 'Multiple user matches',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::MultiUserMatch,
                organization_npi: @organization.npi,
                user_identifier: user_info['sub'],
                invitation: @invitation.id)
      raise MultiUserMatchError, 'too many matching AO users'
    end

    candidates.first || create_new_user(user_info)
  end

  def find_ao_candidates(user_info)
    [
      find_user_by_pac_id,
      find_user_by_uuid(user_info),
      find_user_by_email(user_info)
    ].compact.uniq
  end

  def find_user_by_pac_id
    User.find_by(pac_id: session[:user_pac_id]) if session[:user_pac_id].present?
  end

  # Queries through user_emails table, raises on multiple matches
  # Additional updates to this logic will be handled in DPC-5564
  def find_user_by_email(user_info)
    return nil if user_info['email'].blank?

    users = User.find_by_email_in_user_emails(user_info['email'])

    if users.size > 1
      log_event(:error, 'Multiple user matches',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::MultiUserMatch,
                organization_npi: @organization.npi,
                user_identifier: user_info['sub'],
                invitation: @invitation.id)
      raise MultiUserMatchError, 'too many users matching email'
    end

    users.first
  end

  # Shared helper: create new user for both CD and AO flows
  def create_new_user(user_info)
    User.new.tap do |user|
      assign_user_attributes(user, user_info)
      user.save!
      log_create_user(user_info)
    end
  end

  def assign_user_attributes(user_to_create, user_info)
    user_to_create.given_name = user_info['given_name']
    user_to_create.family_name = user_info['family_name']
    user_to_create.pac_id = session[:user_pac_id]
  end

  def update_user(user_info)
    pac_id = session.delete(:user_pac_id)
    @user.pac_id ||= pac_id
    @user.given_name = user_info['given_name']
    @user.family_name = user_info['family_name']
    @user.save
  end

  def load_invitation
    @invitation = Invitation.find(params[:id])
    if @organization != @invitation.provider_organization
      invalid_status = @invitation.credential_delegate? ? 'cd_invalid' : 'ao_invalid'
      render(Page::Utility::ErrorComponent.new(@invitation, invalid_status), status: :not_found)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::Utility::ErrorComponent.new(@invitation, 'ao_invalid'), status: :not_found)
  end

  def validate_invitation
    return unless @invitation.unacceptable_reason

    err_msg, action_type = invitation_log_data(@invitation.unacceptable_reason)
    log_event(:info, err_msg,
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: action_type,
              organization_npi: @organization.npi,
              invitation: @invitation.id)

    render(Page::Utility::ErrorComponent.new(@invitation, @invitation.unacceptable_reason),
           status: :forbidden)
  end

  def invitation_log_data(reason)
    unacceptable_reason_map = {
      ao_invalid: ['AO Invalid Invitation', LoggingConstants::ActionType::InvalidInvitation],
      cd_invalid: ['CD Invalid Invitation', LoggingConstants::ActionType::InvalidInvitation],
      ao_renewed: ['Ao Renewed Expired Invitation', LoggingConstants::ActionType::AoRenewedExpiredInvitation],
      ao_accepted: ['Authorized Official Invitation already accepted',
                    LoggingConstants::ActionType::AoAlreadyRegistered],
      cd_accepted: ['Credential Delegate Invitation already accepted',
                    LoggingConstants::ActionType::CdAlreadyRegistered],
      ao_expired: ['Authorized Official Invitation expired', LoggingConstants::ActionType::AoInvitationExpired],
      cd_expired: ['Credential Delegate Invitation expired', LoggingConstants::ActionType::CdInvitationExpired]
    }
    unacceptable_reason_map.default = ["Invitation unacceptable: #{reason}",
                                       LoggingConstants::ActionType::UnacceptableInvitation]
    unacceptable_reason_map[reason.to_sym]
  end

  def verify_ao_invitation
    redirect_to organization_invitation_url(@organization, @invitation) unless @invitation.authorized_official?
  end

  def verify_cd_invitation
    redirect_to organization_invitation_url(@organization, @invitation) unless @invitation.credential_delegate?
  end

  def check_for_token
    return if csp_session.active?

    render(Page::Invitations::InvitationLoginComponent.new(@invitation))
  end

  def block_test_utilities
    render plain: :forbidden, status: :forbidden unless Rails.env.test?
  end

  def log_invitation_flow_start
    if @invitation.credential_delegate?
      log_event(:info, 'Credential Delegate invitation flow started,',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::CdInvitationFlowStarted,
                organization_npi: @organization.npi,
                invitation: @invitation.id)
    elsif @invitation.authorized_official?
      log_event(:info, 'Authorized Official invitation flow started,',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::AoInvitationFlowStarted,
                organization_npi: @organization.npi,
                invitation: @invitation.id)
    end
  end

  def log_ao_verification_error(user_info, error, service_unavailable)
    if service_unavailable
      log_event(:error, 'CPI API Gateway unavailable',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::FailCpiApiGwCheck,
                user_identifier: user_info&.dig('sub'),
                error: error.message,
                organization_npi: @organization.npi,
                invitation: @invitation.id)
    else
      log_event(:info, 'AO Check Fail',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::FailCpiApiGwCheck,
                user_identifier: user_info&.dig('sub'),
                verificationReason: error.message,
                organization_npi: @organization.npi,
                invitation: @invitation.id)
    end
  end

  def log_create_user(user_info)
    if @invitation.credential_delegate?
      log_event(:info, 'Credential Delegate user created,',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::CdCreated,
                user_identifier: user_info&.dig('sub'),
                organization_npi: @organization.npi,
                invitation: @invitation.id)
    elsif @invitation.authorized_official?
      log_event(:info, 'Authorized Official user created,',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::AoCreated,
                user_identifier: user_info&.dig('sub'),
                organization_npi: @organization.npi,
                invitation: @invitation.id)
    end
  end

  def log_pii_mismatch(user_info)
    if @invitation.credential_delegate?
      log_event(:info, 'CD PII Check Fail',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::FailCdPiiCheck,
                user_identifier: user_info&.dig('sub'),
                invitation: @invitation.id)
    else
      log_event(:info, 'AO PII Check Fail',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::FailAoPiiCheck,
                user_identifier: user_info&.dig('sub'),
                invitation: @invitation.id)
    end
  end

  def log_waivers(role_and_waivers)
    if role_and_waivers[:has_org_waiver]
      log_event(:info, 'Organization has a waiver',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::OrgHasWaiver,
                invitation: @invitation.id)
    end
    return unless role_and_waivers[:has_ao_waiver]

    log_event(:info, 'Authorized official has a waiver',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::AoHasWaiver,
              invitation: @invitation.id)
  end
end
