# frozen_string_literal: true

# Shows Credential Delegates info about the organizations they manage the credentials for
class OrganizationsController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization, only: %i[index show]

  def index
    @organizations = [@organization]
    render(Page::Organization::OrganizationListComponent.new(organizations: @organizations))
  end

  def show
    if params[:ao]
      provider_organization = ProviderOrganization.find_by(dpc_api_organization_id: @organization.api_id)
      @invitations = Invitation.where(provider_organization:,
                                      invited_by: current_user)
      render(Page::CredentialDelegate::ListComponent.new(@organization, @invitations, []))
    else
      render(Page::Organization::ShowComponent.new(@organization))
    end
  end

  def new
    render(Page::Organization::NewOrganizationComponent.new)
  end

  def create
    @npi_error = npi_error
    return redirect_to tos_form_organization_path('place-holder') unless @npi_error.present?

    render(Page::Organization::NewOrganizationComponent.new(@npi_error), status: :bad_request)
  end

  def tos_form
    organization = ProviderOrganization.new(npi: '1111111111', name: 'Health Hut')
    render(Page::Organization::TosFormComponent.new(organization))
  end

  def sign_tos
    redirect_to success_organization_path('place-holder')
  end

  def success
    render(Page::Organization::NewOrganizationSuccessComponent.new(params[:npi]))
  end

  def npi_error
    if params[:npi].blank?
      "can't be blank"
    elsif params[:npi].length != 10
      'length has to be 10'
    end
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
                      Organization.new(params[:id])
                    end
  rescue DpcRecordNotFound
    render file: "#{Rails.root}/public/404.html", layout: false, status: :not_found
  end
end
