# frozen_string_literal: true

# Handles acceptance of invitations
class InvitationsController < ApplicationController
  before_action :load_organization
  before_action :load_invitation, only: %i[accept confirm]
  before_action :authenticate_user!
  before_action :invitation_matches_cd, only: %i[confirm]

  def accept
    if current_user.email != @cd_invitation.invited_email
      return render(Page::CredentialDelegate::BadInvitationComponent.new('pii_mismatch'),
                    status: :forbidden)
    end

    render(Page::CredentialDelegate::AcceptInvitationComponent.new(@organization, @cd_invitation))
  end

  def confirm
    CdOrgLink.create!(user: current_user, provider_organization: @organization, invitation: @cd_invitation)
    @cd_invitation.update!(invited_given_name: nil, invited_family_name: nil, invited_phone: nil, invited_email: nil)
    flash[:notice] = "Invitation accepted. You can now manage this organization's credentials. Learn more."
    redirect_to organizations_path
  end

  def login
    session['omniauth.nonce'] = nonce = SecureRandom.hex(16)
    session['omniauth.state'] = state = SecureRandom.hex(16)
    url = URI::HTTPS.build(host: 'idp.int.identitysandbox.gov',
                           path: '/openid_connect/authorize',
                           query: { acr_values: 'http://idmanagement.gov/ns/assurance/ial/2',
                                    client_id: 'urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:local',
                                    redirect_uri: 'http://localhost:3100/portal/users/auth/openid_connect/callback',
                                    response_type: 'code',
                                    scope: 'openid email profile phone social_security_number',
                                    nonce: nonce,
                                    state: state
                                  }.to_query)
    redirect_to url, allow_other_host: true
  end

  private

  def authenticate_user!
    return if current_user

    render(Page::Session::InvitationLoginComponent.new(@cd_invitation))
  end

  def invitation_matches_cd
    unless @cd_invitation.match_user?(current_user)
      return render(Page::CredentialDelegate::BadInvitationComponent.new('pii_mismatch'),
                    status: :forbidden)
    end
    return if params[:verification_code] == @cd_invitation.verification_code

    @cd_invitation.errors.add(:verification_code, :bad_code, message: 'tbd')
    render(Page::CredentialDelegate::AcceptInvitationComponent.new(@organization, @cd_invitation),
           status: :bad_request)
  end

  def load_invitation
    @cd_invitation = Invitation.find(params[:id])
    if @organization != @cd_invitation.provider_organization
      render(Page::CredentialDelegate::BadInvitationComponent.new('invalid'), status: :not_found)
    elsif @cd_invitation.expired? || @cd_invitation.accepted? || @cd_invitation.cancelled_at.present?
      render(Page::CredentialDelegate::BadInvitationComponent.new('invalid'),
             status: :forbidden)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::CredentialDelegate::BadInvitationComponent.new('invalid'), status: :not_found)
  end
end
