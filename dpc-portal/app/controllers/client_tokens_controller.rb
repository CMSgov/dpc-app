# frozen_string_literal: true

# Hanles client token requests
class ClientTokensController < ApplicationController
  before_action :authenticate_user!
  before_action :check_user_verification
  before_action :load_organization
  before_action :require_can_access
  before_action :tos_accepted

  def new
    render Page::ClientToken::NewTokenComponent.new(@organization)
  end

  def create
    return render_error 'Label required.' unless params_present?

    manager = ClientTokenManager.new(@organization.dpc_api_organization_id)
    if manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      log_credential_action(:client_token, :add)
      render(Page::ClientToken::ShowTokenComponent.new(@organization, @client_token))
    else
      logger.error(['Unable to create client token', JSON.parse(manager.client_token || '{}')])
      render_error 'Client token could not be created.'
    end
  end

  def destroy
    manager = ClientTokenManager.new(@organization.dpc_api_organization_id)
    if manager.delete_client_token(id: params[:id])
      flash[:notice] = 'Client token successfully deleted.'
      log_credential_action(:client_token, :remove)
    else
      flash[:alert] = 'Client token could not be deleted.'
    end
    redirect_to organization_path(@organization)
  end

  private

  def params_present?
    params[:label].present?
  end

  def render_error(msg)
    flash.now.alert = msg
    render Page::ClientToken::NewTokenComponent.new(@organization)
  end
end
