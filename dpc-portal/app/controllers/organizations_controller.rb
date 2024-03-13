# frozen_string_literal: true

# Shows Credential Delegates info about the organizations they manage the credentials for
class OrganizationsController < ApplicationController
  before_action :block_prod_sbx
  before_action :authenticate_user!
  before_action :load_organization, only: %(show)

  def index
    @organizations = []
    render(Page::Organization::OrganizationListComponent.new(organizations: @organizations))
  end

  def show
    if params[:ao]
      @invitations = Invitation.where(provider_organization: @organization,
                                      invited_by: current_user)
      @cds = CdOrgLink.where(provider_organization: @organization,
                              disabled_at: nil)
      render(Page::CredentialDelegate::ListComponent.new(@organization, @invitations, @cds))
    else
      render(Page::Organization::ShowComponent.new(@organization))
    end
  end

  private

  def load_organization
    @organization = ProviderOrganization.find(params[:id])
    redirect_to organizations_path unless current_user.can_access?(@organization)
  rescue ActiveRecord::RecordNotFound
    render file: "#{Rails.root}/public/404.html", layout: false, status: :not_found
  end
end
