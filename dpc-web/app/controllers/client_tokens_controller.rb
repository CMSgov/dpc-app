# frozen_string_literal: true
class ClientTokensController < ApplicationController
  # before_action :authenticate_user!
  before_action :organization_enabled?
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @organization = current_user.organizations.find(org_id)
  end

  def create
    @organization = current_user.organizations.find(org_id)

    reg_org = @organization.registered_organization
    manager = ClientTokenManager.new(registered_organization: reg_org)

    if params_present? && manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      render :show
    else
      return render_error 'Label required.' unless params_present?

      render_error 'Client token could not be created.'
    end
  end

  def destroy
    @organization = current_user.organizations.find(org_id)
    reg_org = @organization.registered_organization

    manager = ClientTokenManager.new(registered_organization: reg_org)
    if manager.delete_client_token(id: params[:id])
      flash[:notice] = 'Client token successfully deleted.'
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
    flash[:alert] = msg
    render :new
  end

  def params_present?
    params[:label].present?
  end

  def unauthorized
    flash[:error] = 'Unauthorized'
    redirect_to portal_path
  end
end
