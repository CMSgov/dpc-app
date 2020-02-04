# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def create
    @organization = current_user.organizations.find(params[:organization_id])
    return render_error('Must have both a label and an API environment.') if missing_invalid_params

    reg_org = @organization.registered_organizations.find_by(api_env: params[:api_environment])
    manager = ClientTokenManager.new(api_env: params[:api_environment], registered_organization: reg_org)
    if manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      render :show
    else
      render_error 'Client token could not be created.'
    end
  end

  private

  def render_error(msg)
    flash[:error] = msg
    render :new
  end

  def missing_invalid_params
    params[:api_environment].blank? || params[:label].blank?
  end

  def unauthorized
    flash[:error] = 'Unauthorized'
    redirect_to dashboard_path
  end
end
