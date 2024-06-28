# frozen_string_literal: true

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  before_action :load_organization
  before_action :load_invitation
  before_action :validate_invitation, except: %i[renew]
  before_action :user_logged_in, except: %i[login renew show]
  before_action :invitation_matches_user, only: %i[accept]
  before_action :invitation_matches_conditions, only: %i[confirm]

  def show
    render(Page::Invitations::StartComponent.new(@organization, @invitation))
  end

  def accept
    session["invitation_status_#{@invitation.id}"] = 'identity_verified'
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation))
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

    if @invitation.credential_delegate?
      create_cd_org_link
    elsif @invitation.authorized_official?
      create_ao_org_link
    else
      return render(Page::Invitations::BadInvitationComponent.new(@invitation, 'invalid', 'warning'),
                    status: :unprocessable_entity)
    end
    session.delete("invitation_status_#{@invitation.id}")
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

  def create_cd_org_link
    CdOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    @invitation.accept!
    flash[:notice] = "Invitation accepted. You can now manage this organization's credentials. Learn more."
  end

  def create_ao_org_link
    AoOrgLink.create!(user:, provider_organization: @organization, invitation: @invitation)
    @invitation.accept!
    flash[:notice] = 'Invitation accepted.'
  end

  def user
    auth = UserInfoService.new.user_info(session)
    User.find_or_create_by(provider: :openid_connect, uid: auth['sub']) do |user_to_create|
      user_to_create.email = auth['email']
      # Assign random, acceptable password to keep Devise happy.
      # User should log in only through IdP
      user_to_create.password = user_to_create.password_confirmation = Devise.friendly_token[0, 20]
    end
  end

  def user_logged_in
    if session[:login_dot_gov_token].present? &&
       session[:login_dot_gov_token_exp].present? &&
       session[:login_dot_gov_token_exp] > Time.now
      return
    end

    render(Page::Session::InvitationLoginComponent.new(@invitation))
  end

  def invitation_matches_user
    user_info = UserInfoService.new.user_info(session)
    unless @invitation.match_user?(user_info)
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'pii_mismatch', 'error'),
             status: :forbidden)
    end
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e)
  end

  def invitation_matches_conditions
    unless session["invitation_status_#{@invitation.id}"] == 'identity_verified'
      return redirect_to accept_organization_invitation_url(@organization,
                                                            @invitation)
    end

    return check_code if @invitation.credential_delegate?

    user_info = UserInfoService.new.user_info(session)
    @invitation.ao_match?(user_info)
  rescue InvitationError => e
    render(Page::Invitations::BadInvitationComponent.new(@invitation, e.message, 'error'),
           status: :forbidden)
  end

  def check_code
    return if params[:verification_code] == @invitation.verification_code

    @invitation.errors.add(:verification_code, :bad_code, message: 'tbd')
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation),
           status: :bad_request)
  end

  def handle_user_info_service_error(error)
    case error.message
    when 'unauthorized'
      render(Page::Session::InvitationLoginComponent.new(@invitation))
    else
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'server_error', 'warning'),
             status: :service_unavailable)
    end
  end

  def load_invitation
    @invitation = Invitation.find(params[:id])
    if @organization != @invitation.provider_organization
      render(Page::Invitations::BadInvitationComponent.new(@invitation, 'invalid', 'warning'), status: :not_found)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::Invitations::BadInvitationComponent.new(@invitation, 'invalid', 'warning'), status: :not_found)
  end

  def validate_invitation
    return unless @invitation.unacceptable_reason

    render(Page::Invitations::BadInvitationComponent.new(@invitation, @invitation.unacceptable_reason, 'warning'),
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
