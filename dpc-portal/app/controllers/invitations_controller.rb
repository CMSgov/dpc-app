# frozen_string_literal: true

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  before_action :load_organization
  before_action :load_invitation
  before_action :validate_invitation, except: %i[renew]
  before_action :verify_ao_invitation, only: %i[accept confirm]
  before_action :verify_cd_invitation, only: %i[code verify_code confirm_cd]
  before_action :check_for_token, only: %i[accept confirm confirm_cd register]

  def show
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
    Rails.logger.info(['Approved access authorization occurred for the Credential Delegate',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::CdConfirmed }])
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation, @given_name, @family_name))
  end

  # Everybody
  def register
    unless session["invitation_status_#{@invitation.id}"] == 'verification_complete'
      return redirect_to organization_invitation_url(@organization, @invitation)
    end

    return unless create_link

    session.delete("invitation_status_#{@invitation.id}")
    sign_in(:user, @user)
    Rails.logger.info(['User logged in',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::UserLoggedIn }])
    render(Page::Invitations::SuccessComponent.new(@organization, @invitation, @given_name, @family_name))
  end

  def login
    login_session
    Rails.logger.info(['User began login flow',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::BeginLogin }])
    url = URI::HTTPS.build(host: IDP_HOST,
                           path: '/openid_connect/authorize',
                           query: { acr_values: 'http://idmanagement.gov/ns/assurance/ial/2',
                                    client_id: IDP_CLIENT_ID,
                                    redirect_uri: "#{redirect_host}/portal/users/auth/openid_connect/callback",
                                    response_type: 'code',
                                    scope: 'openid email all_emails profile social_security_number',
                                    nonce: @nonce,
                                    state: @state }.to_query)
    redirect_to url, allow_other_host: true
  end

  def renew
    if @invitation.renew
      flash[:notice] = 'You should receive your new invitation shortly'
    else
      flash[:alert] = 'Unable to create new invitation'
    end
    redirect_to accept_organization_invitation_url(@organization, @invitation)
  end

  private

  def invitation_matches_user
    user_info = UserInfoService.new.user_info(session)
    render_if_bad_invitation(user_info)
    session["invitation_status_#{@invitation.id}"] = 'identity_verified'
    @given_name = user_info['given_name']
    @family_name = user_info['family_name']
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 1)
  end

  def render_if_bad_invitation(user_info)
    if @invitation.credential_delegate? && !@invitation.cd_match?(user_info)
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'pii_mismatch'),
             status: :forbidden)
    elsif !@invitation.email_match?(user_info)
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'email_mismatch'),
             status: :forbidden)
    end
  end

  def verify_user_is_ao
    user_info = UserInfoService.new.user_info(session)
    result = @invitation.ao_match?(user_info) # raises if does not match
    session[:user_pac_id] = result.dig(:ao_role, 'pacId')
    log_waivers(result)
  rescue VerificationError => e
    status = AoVerificationService::SERVER_ERRORS.include?(e.message) ? :service_unavailable : :forbidden
    log_ao_verification_error(e, status == :service_unavailable)
    render(Page::Invitations::AoFlowFailComponent.new(@invitation, e.message, 2), status:)
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 2)
  end

  def handle_user_info_service_error(error, step)
    logger.error(['User Info Service unavailable',
                  { actionContext: LoggingConstants::ActionContext::Registration, error: error.message }])

    if error.message == 'unauthorized'
      render(Page::Invitations::InvitationLoginComponent.new(@invitation))
    elsif @invitation.credential_delegate?
      render(Page::Invitations::BadInvitationComponent.new(@invitation, error.message),
             status: :service_unavailable)
    else
      render(Page::Invitations::AoFlowFailComponent.new(@invitation, error.message, step),
             status: :service_unavailable)
    end
  end

  def login_session
    session[:user_return_to] = if @invitation.authorized_official?
                                 accept_organization_invitation_url(@organization, params[:id])
                               else
                                 confirm_cd_organization_invitation_url(@organization, params[:id])
                               end
    session['omniauth.nonce'] = @nonce = SecureRandom.hex(16)
    session['omniauth.state'] = @state = SecureRandom.hex(16)
  end

  def create_link
    if @invitation.credential_delegate?
      create_cd_org_link
    elsif @invitation.authorized_official?
      create_ao_org_link
    else
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'invalid'),
             status: :unprocessable_entity)
      false
    end
  end

  def create_cd_org_link
    CdOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    Rails.logger.info(['Credential Delegate linked to organization',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::CdLinkedToOrg }])
    @invitation.accept!
  end

  def create_ao_org_link
    AoOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    Rails.logger.info(['Authorized Official linked to organization',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::AoLinkedToOrg }])
    @invitation.accept!
    @user.update(verification_status: 'approved')
    @organization.update(verification_status: 'approved')
  end

  def user
    user_info = UserInfoService.new.user_info(session)
    @user = User.find_or_create_by!(provider: :openid_connect, uid: user_info['sub']) do |user_to_create|
      assign_user_attributes(user_to_create, user_info)
      log_create_user
    end
    update_user(user_info)
    @user
  end

  def assign_user_attributes(user_to_create, user_info)
    user_to_create.email = @invitation.invited_email
    user_to_create.given_name = user_info['given_name']
    user_to_create.family_name = user_info['family_name']
    user_to_create.pac_id = session.delete(:user_pac_id)
  end

  def update_user(user_info)
    @user.pac_id = session.delete(:user_pac_id) unless @user.pac_id
    @user.given_name = user_info['given_name']
    @user.family_name = user_info['family_name']
    @user.save
  end

  def load_invitation
    @invitation = Invitation.find(params[:id])
    if @organization != @invitation.provider_organization
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'invalid'), status: :not_found)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::Invitations::BadInvitationComponent.new(@invitation, 'invalid'), status: :not_found)
  end

  def validate_invitation
    return unless @invitation.unacceptable_reason

    if @invitation.credential_delegate?
      Rails.logger.info(['Credential Delegate Invitation expired',
                         { actionContext: LoggingConstants::ActionContext::Registration,
                           actionType: LoggingConstants::ActionType::CdInvitationExpired }])
    elsif @invitation.authorized_official?
      Rails.logger.info(['Authorized Official Invitation expired',
                         { actionContext: LoggingConstants::ActionContext::Registration,
                           actionType: LoggingConstants::ActionType::AoInvitationExpired }])
    end
    render(Page::Invitations::BadInvitationComponent.new(@invitation, @invitation.unacceptable_reason),
           status: :forbidden)
  end

  def verify_ao_invitation
    redirect_to organization_invitation_url(@organization, @invitation) unless @invitation.authorized_official?
  end

  def verify_cd_invitation
    redirect_to organization_invitation_url(@organization, @invitation) unless @invitation.credential_delegate?
  end

  def check_for_token
    if session[:login_dot_gov_token].present? &&
       session[:login_dot_gov_token_exp].present? &&
       session[:login_dot_gov_token_exp] > Time.now
      return
    end

    render(Page::Invitations::InvitationLoginComponent.new(@invitation))
  end

  def log_ao_verification_error(error, service_unavailable)
    if service_unavailable
      logger.error(['CPI API Gateway unavailable',
                    { actionContext: LoggingConstants::ActionContext::Registration, error: error.message }])
    else
      logger.info(['AO Check Fail',
                   { actionContext: LoggingConstants::ActionContext::Registration,
                     actionType: LoggingConstants::ActionType::FailCpiApiGwCheck,
                     verificationReason: error.message,
                     invitation: @invitation.id }])
    end
  end

  def log_create_user
    if @invitation.credential_delegate?
      Rails.logger.info(['Credential Delegate user created,',
                         { actionContext: LoggingConstants::ActionContext::Registration,
                           actionType: LoggingConstants::ActionType::CdCreated }])
    elsif @invitation.authorized_official?
      Rails.logger.info(['Authorized Official user created,',
                         { actionContext: LoggingConstants::ActionContext::Registration,
                           actionType: LoggingConstants::ActionType::AoCreated }])
    end
  end

  def log_waivers(role_and_waivers)
    if role_and_waivers[:has_org_waiver]
      Rails.logger.info(['Organization has a waiver',
                         { actionContext: LoggingConstants::ActionContext::Registration,
                           actionType: LoggingConstants::ActionType::OrgHasWaiver,
                           invitation: @invitation.id }])
    end
    return unless role_and_waivers[:has_ao_waiver]

    Rails.logger.info(['Authorized official has a waiver',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::AoHasWaiver,
                         invitation: @invitation.id }])
  end
end

def redirect_host
  case ENV.fetch('ENV', nil)
  when 'local'
    'http://localhost:3100'
  when 'prod'
    'https://dpc.cms.gov'
  else
    "https://#{ENV.fetch('ENV', nil)}.dpc.cms.gov"
  end
end
