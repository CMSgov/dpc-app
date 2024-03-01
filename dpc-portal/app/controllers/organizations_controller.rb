# frozen_string_literal: true

# Shows Credential Delegates info about the organizations they manage the credentials for
class OrganizationsController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization

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
end
