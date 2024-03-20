# frozen_string_literal: true

# Manages invitations to become a Credential Delegate
class CredentialDelegateInvitationsController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization
  before_action :require_ao, only: %i[new create success]
  before_action :load_invitation, only: %i[accept confirm]
  before_action :invitation_matches_cd, only: %i[confirm]

  def new
    render(Page::CredentialDelegate::NewInvitationComponent.new(@organization, Invitation.new))
  end

  def create
    permitted = params.permit(:invited_given_name, :invited_family_name, :phone_raw, :invited_email,
                              :invited_email_confirmation)
    @cd_invitation = Invitation.new(**permitted.to_h,
                                    provider_organization: @organization,
                                    invitation_type: 'credential_delegate',
                                    invited_by: current_user,
                                    verification_code: (Array('A'..'Z') + Array(0..9)).sample(6).join)
    if @cd_invitation.save
      redirect_to success_organization_credential_delegate_invitation_path(@organization.path_id, 'new-invitation')
    else
      render(Page::CredentialDelegate::NewInvitationComponent.new(@organization, @cd_invitation), status: :bad_request)
    end
  end

  def success
    render(Page::CredentialDelegate::InvitationSuccessComponent.new(@organization))
  end

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

  private

  def invitation_matches_cd
    unless @cd_invitation.match_user?(current_user)
      return render(Page::CredentialDelegate::BadInvitationComponent.new('pii_mismatch'),
                    status: :forbidden)
    end
    return unless params[:verification_code] != @cd_invitation.verification_code

    @cd_invitation.errors.add(:verification_code, :bad_code, message: 'tbd')
    render(Page::CredentialDelegate::AcceptInvitationComponent.new(@organization, @cd_invitation),
           status: :bad_request)
  end

  def load_invitation
    @cd_invitation = Invitation.find(params[:id])
    if @cd_invitation.expired? || @cd_invitation.accepted? || @cd_invitation.cancelled_at.present?
      render(Page::CredentialDelegate::BadInvitationComponent.new('invalid'),
             status: :forbidden)
    end
  rescue ActiveRecord::RecordNotFound
    render(Page::CredentialDelegate::BadInvitationComponent.new('invalid'), status: :not_found)
  end
end
