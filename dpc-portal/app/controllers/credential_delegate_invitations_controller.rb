# frozen_string_literal: true

# Manages invitations to become a Credential Delegate
class CredentialDelegateInvitationsController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization

  def new
    render(Page::CredentialDelegate::NewInvitationComponent.new(@organization, CdInvitation.new))
  end

  def create
    permitted = params.permit(:given_name, :family_name, :phone_raw, :email, :email_confirmation)
    cd_invitation = CdInvitation.new(**permitted.to_h)
    if cd_invitation.valid?
      redirect_to success_organization_credential_delegate_invitation_path(@organization.path_id, 'new-invitation')
    else
      render(Page::CredentialDelegate::NewInvitationComponent.new(@organization, cd_invitation), status: :bad_request)
    end
  end

  def success
    render(Page::CredentialDelegate::InvitationSuccessComponent.new(@organization))
  end

  private

  def load_organization
    @organization = case ENV.fetch('ENV', nil)
                    when 'prod-sbx'
                      redirect_to root_url
                    when 'test'
                      Organization.new('6a1dbf47-825b-40f3-b81d-4a7ffbbdc270')
                    when 'dev'
                      Organization.new('78d02106-2837-4d07-8c51-8d73332aff09')
                    else
                      Organization.new(params[:organization_id])
                    end
  end
end
