# frozen_string_literal: true

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  before_action :load_organization
  before_action :load_invitation
  before_action :authenticate_user!, except: %i[login]
  before_action :invitation_matches_cd, only: %i[confirm]

  def accept
    if current_user.email != @cd_invitation.invited_email
      return render(Page::Invitations::BadInvitationComponent.new('pii_mismatch'),
                    status: :forbidden)
    end

    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @cd_invitation))
  end

  def confirm
    CdOrgLink.create!(user: current_user, provider_organization: @organization, invitation: @cd_invitation)
    @cd_invitation.update!(invited_given_name: nil, invited_family_name: nil, invited_phone: nil, invited_email: nil)
    flash[:notice] = "Invitation accepted. You can now manage this organization's credentials. Learn more."
    redirect_to organizations_path
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

  def authenticate_user!
    return if current_user

    render(Page::Session::InvitationLoginComponent.new(@cd_invitation))
  end

  def invitation_matches_cd
    unless @cd_invitation.match_user?(current_user)
      return render(Page::Invitations::BadInvitationComponent.new('pii_mismatch'),
                    status: :forbidden)
    end
    return if params[:verification_code] == @cd_invitation.verification_code

    @cd_invitation.errors.add(:verification_code, :bad_code, message: 'tbd')
    render(Page::Invitations::AcceptInvitationComponent.new(@organization, @cd_invitation),
           status: :bad_request)
  end

  def load_invitation
    @cd_invitation = Invitation.find(params[:id])
    if @organization != @cd_invitation.provider_organization
      render(Page::Invitations::BadInvitationComponent.new('invalid'), status: :not_found)
    elsif @cd_invitation.expired? || @cd_invitation.accepted? || @cd_invitation.cancelled_at.present?
      render(Page::Invitations::BadInvitationComponent.new('invalid'),
             status: :forbidden)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::Invitations::BadInvitationComponent.new('invalid'), status: :not_found)
  end

  def login_session
    session[:user_return_to] = accept_organization_invitation_url(@organization, params[:id])
    session['omniauth.nonce'] = @nonce = SecureRandom.hex(16)
    session['omniauth.state'] = @state = SecureRandom.hex(16)
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
end
