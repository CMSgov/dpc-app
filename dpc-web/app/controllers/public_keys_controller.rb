# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def create
    @organization = current_user.organizations.find(params[:organization_id])
    return render_error('Must have both a label and an API environment.') if missing_invalid_params

    reg_org = @organization.registered_organizations.find_by(api_env: params[:api_environment])
    manager = PublicKeyManager.new(api_env: params[:api_environment], registered_organization: reg_org)
    if manager.create_public_key(public_key: params[:public_key], label: params[:label])
      redirect_to dashboard_path
    else
      render_error 'Public key could not be created.'
    end
  end

  private

  def render_error(msg)
    flash[:error] = msg
    render :new
  end

  def missing_invalid_params
    params[:api_environment].blank? || params[:public_key].blank?
  end

  def unauthorized
    flash[:error] = 'Unauthorized'
    redirect_to dashboard_path
  end
end
