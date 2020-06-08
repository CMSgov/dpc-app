# frozen_string_literal: true

class PublicKeysController < ApplicationController
  layout 'public-key-new'
  before_action :authenticate_user!
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def create
    @organization = current_user.organizations.find(params[:organization_id])
    return render_error('Required values missing.') if missing_invalid_params

    reg_org = @organization.registered_organizations.find_by(api_env: params[:api_environment])
    manager = PublicKeyManager.new(api_env: params[:api_environment], registered_organization: reg_org)

    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      label: params[:label],
      snippet_signature: params[:snippet_signature]
    )

    if new_public_key[:response]
      redirect_to dashboard_path
    else
      render_error new_public_key[:message]
    end
  end

  private

  def render_error(msg)
    flash[:alert] = msg
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
