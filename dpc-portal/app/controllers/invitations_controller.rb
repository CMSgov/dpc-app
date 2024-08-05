# frozen_string_literal: true

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  before_action :load_organization
  before_action :load_invitation
  before_action :validate_invitation, except: %i[renew]
  before_action :check_for_token, except: %i[login renew show]
  before_action :invitation_matches_user, only: %i[accept]
  before_action :invitation_matches_conditions, only: %i[confirm]

  def show
    render(Page::Invitations::StartComponent.new(@organization, @invitation))
  end

  def accept
    session["invitation_status_#{@invitation.id}"] = 'identity_verified'
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation, @given_name, @family_name))
  end

  def confirm
    session["invitation_status_#{@invitation.id}"] = 'conditions_verified'
    render(Page::Invitations::RegisterComponent.new(@organization, @invitation))
  end

  def register
    unless session["invitation_status_#{@invitation.id}"] == 'conditions_verified'
      return redirect_to accept_organization_invitation_url(@organization,
                                                            @invitation)
    end

    return unless create_link

    session.delete("invitation_status_#{@invitation.id}")
    sign_in(:user, @user)
    render(Page::Invitations::SuccessComponent.new(@organization, @invitation))
  end

  def login
    login_session
    client_id = "urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:#{ENV.fetch('ENV')}"
    url = URI::HTTPS.build(host: ENV.fetch('IDP_HOST'),
                           path: '/openid_connect/authorize',
                           query: { acr_values: 'http://idmanagement.gov/ns/assurance/ial/2',
                                    client_id:,
                                    redirect_uri: "#{redirect_host}/portal/users/auth/openid_connect/callback",
                                    response_type: 'code',
                                    scope: 'openid email all_emails profile phone social_security_number',
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
    logger.info('Credential Delegate created and linked to organization',
                actionContext: LoggingConstants::ActionContext::Registration,
                actionType: LoggingConstants::ActionType::CdCreatedAndLinkedToOrg)
    @invitation.accept!
  end

  def create_ao_org_link
    AoOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    logger.info('Authorized Official created and linked to organization',
                actionContext: LoggingConstants::ActionContext::Registration,
                actionType: LoggingConstants::ActionType::AoCreatedAndLinkedToOrg)
    @invitation.accept!
  end

  def user
    user_info = UserInfoService.new.user_info(session)
    @user = User.find_or_create_by!(provider: :openid_connect, uid: user_info['sub']) do |user_to_create|
      user_to_create.email = @invitation.invited_email
      user_to_create.pac_id = session.delete(:user_pac_id)
    end
    @user.update(pac_id: session.delete(:user_pac_id)) unless @user.pac_id
    @user
  end

  def check_for_token
    if session[:login_dot_gov_token].present? &&
       session[:login_dot_gov_token_exp].present? &&
       session[:login_dot_gov_token_exp] > Time.now
      return
    end

    render(Page::Invitations::InvitationLoginComponent.new(@invitation))
  end

  def invitation_matches_user
    user_info = UserInfoService.new.user_info(session)
    unless @invitation.match_user?(user_info)
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'pii_mismatch'),
             status: :forbidden)
    end
    @given_name = user_info['given_name']
    @family_name = user_info['family_name']
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 1)
  end

  def invitation_matches_conditions
    unless session["invitation_status_#{@invitation.id}"] == 'identity_verified'
      return redirect_to accept_organization_invitation_url(@organization, @invitation)
    end

    return check_code if @invitation.credential_delegate?

    check_ao
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e, 2)
  rescue InvitationError => e
    status = AoVerificationService::SERVER_ERRORS.include?(e.message) ? :service_unavailable : :forbidden
    render(Page::Invitations::AoFlowFailComponent.new(@invitation, e.message, 2), status:)
  end

  def check_code
    return if params[:verification_code] == @invitation.verification_code

    @invitation.errors.add(:verification_code, :bad_code, message: 'tbd')
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation, @given_name, @family_name),
           status: :bad_request)
  end

  def check_ao
    user_info = UserInfoService.new.user_info(session)
    result = @invitation.ao_match?(user_info)
    session[:user_pac_id] = result.dig(:ao_role, 'pacId') if result[:success]
    result[:success]
  end

  def handle_user_info_service_error(error, step)
    logger.error "Invitation Flow UserInfoServiceError: #{error.message}"

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

    render(Page::Invitations::BadInvitationComponent.new(@invitation, @invitation.unacceptable_reason),
           status: :forbidden)
  end

  def login_session
    session[:user_return_to] = accept_organization_invitation_url(@organization, params[:id])
    session['omniauth.nonce'] = @nonce = SecureRandom.hex(16)
    session['omniauth.state'] = @state = SecureRandom.hex(16)
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
