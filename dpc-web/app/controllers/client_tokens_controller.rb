# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!
  before_action :organization_enabled?
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @organization = current_user.organizations.find(org_id)
  end

  def create
    @organization = current_user.organizations.find(org_id)
    return render_error('Must have a label.') if missing_params

    reg_org = @organization.registered_organization
    manager = ClientTokenManager.new(registered_organization: reg_org)
    if manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      render :show
    else
      render_error 'Client token could not be created.'
    end
  end

  def destroy
    @organization = current_user.organizations.find(org_id)
    reg_org = @organization.registered_organization
    token_id = params[:id]

    manager = ClientTokenManager.new(registered_organization: reg_org)
    if manager.delete_client_token(id: token_id)
      redirect_to root_path
    else
      render_error 'Client token could not be deleted.'
    end
  end

  def organization_enabled?
    @organization = current_user.organizations.find(org_id)
    @reg_org = @organization.reg_org

    return if @reg_org.present? && @reg_org.enabled == true

    redirect_to root_path
  end

  private

  def org_id
    params[:organization_id]
  end

  def render_error(msg)
    flash[:error] = msg
    render :new
  end

  def missing_params
    params[:label].blank?
  end

  def unauthorized
    flash[:error] = 'Unauthorized'
    redirect_to portal_path
  end
end
