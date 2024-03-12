# frozen_string_literal: true

# Manages invitations to become a Credential Delegate
class CredentialDelegateInvitationsController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization

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

  private

  def load_organization
    api_org_id = case ENV.fetch('ENV', nil)
                 when 'prod-sbx'
                   redirect_to root_url
                 when 'test'
                   '6a1dbf47-825b-40f3-b81d-4a7ffbbdc270'
                 when 'dev'
                   '78d02106-2837-4d07-8c51-8d73332aff09'
                 else
                   params[:organization_id]
                 end
    @organization = ProviderOrganization.find_or_create_by(dpc_api_organization_id: api_org_id) do |org|
      api_org = Organization.new(api_org_id)
      org.name = api_org.name
      org.npi = api_org.npi
    end
  end
end
