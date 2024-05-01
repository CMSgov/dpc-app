# frozen_string_literal: true

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  before_action :load_organization
  before_action :load_invitation
  before_action :authenticate_user!, except: %i[login]
  before_action :invitation_matches_user, only: %i[confirm]

  def accept
    if current_user.email != @invitation.invited_email
      return render(Page::Invitations::BadInvitationComponent.new('pii_mismatch', 'error'),
                    status: :forbidden)
    end

    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @invitation))
  end

  def confirm
    case @invitation.invitation_type
    when 'credential_delegate'
      bind_cd
    when 'authorized_official'
      bind_ao
    else
      return render(Page::Invitations::BadInvitationComponent.new('invalid', 'warning'),
                    status: :unprocessable_entity)
    end
    redirect_to organization_path(@organization)
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

  private

  def bind_cd
    CdOrgLink.create!(user: current_user, provider_organization: @organization, invitation: @invitation)
    @invitation.update!(invited_given_name: nil, invited_family_name: nil, invited_phone: nil, invited_email: nil)
    flash[:notice] = "Invitation accepted. You can now manage this organization's credentials. Learn more."
  end

  def bind_ao
    AoOrgLink.create!(user: current_user, provider_organization: @organization, invitation: @invitation)
    @invitation.update!(invited_given_name: nil, invited_family_name: nil, invited_phone: nil, invited_email: nil)
    flash[:notice] = 'Invitation accepted.'
  end

  def authenticate_user!
    return if current_user

    render(Page::Session::InvitationLoginComponent.new(@invitation))
  end

  def invitation_matches_user
    user_info = UserInfoService.new.user_info(session)
    unless @invitation.match_user?(user_info)
      return render(Page::Invitations::BadInvitationComponent.new('pii_mismatch', 'error'),
                    status: :forbidden)
    end
    maybe_check_code
  rescue UserInfoServiceError => e
    handle_user_info_service_error(e)
  rescue InvitationError => e
    render(Page::Invitations::BadInvitationComponent.new(e.message, 'error'),
           status: :forbidden)
  end

  def maybe_check_code
    return if @invitation.invitation_type == :authorized_official

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
      render(Page::Invitations::BadInvitationComponent.new('server_error', 'warning'),
             status: :service_unavailable)
    end
  end

  def load_invitation
    @invitation = Invitation.find(params[:id])
    if @organization != @invitation.provider_organization
      render(Page::Invitations::BadInvitationComponent.new('invalid', 'warning'), status: :not_found)
    elsif @invitation.expired? || @invitation.accepted? || @invitation.cancelled_at.present?
      render(Page::Invitations::BadInvitationComponent.new('invalid', 'warning'),
             status: :forbidden)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::Invitations::BadInvitationComponent.new('invalid', 'warning'), status: :not_found)
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
