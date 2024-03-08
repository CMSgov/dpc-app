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
      render(Page::CredentialDelegate::ListComponent.new(@organization, []))
    else
      render(Page::Organization::ShowComponent.new(@organization))
    end
  end

  def new
    render(Page::Organization::NewOrganizationComponent.new(''))
  end

  def create
    if npi_error.blank?
      render(Page::Organization::NewOrganizationSuccessComponent.new(params[:npi]))
    else
      render(Page::Organization::NewOrganizationComponent.new(npi_error), status: :bad_request)
    end
  end

  def npi_error
    if params[:npi].blank?
      "can't be blank"
    elsif params[:npi].length != 10
      'length has to be 10'
    else
      ''
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
