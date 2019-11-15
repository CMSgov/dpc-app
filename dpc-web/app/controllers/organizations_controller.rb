# frozen_string_literal: true

class OrganizationsController < ApplicationController
  before_action :authenticate_user!

  def edit
    @organization = current_user.organizations.find(params[:id])
  end

  def update
    @organization = current_user.organizations.find(params[:id])
    if @organization.update organization_params
      flash[:notice] = 'Organization updated.'
      redirect_to dashboard_path
    else
      flash[:alert] = 'Organization could not be updated.'
      render :edit
    end
  end

  private

  def organization_params
    params.fetch(:organization).permit(:npi)
  end
end
