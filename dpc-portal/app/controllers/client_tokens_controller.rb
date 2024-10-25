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
    manager = ClientTokenManager.new(@organization.dpc_api_organization_id)
    new_token = manager.create_client_token(label: params[:label])
    if new_token[:response]
      @client_token = new_token[:message]
      log_credential_action(:client_token, @client_token['id'], :add)
      render(Page::ClientToken::ShowTokenComponent.new(@organization, @client_token))
    else
      @errors = new_token[:errors] || {}
      render_error manager.errors[:root] || 'No token name.'
    end
  end

  def destroy
    manager = ClientTokenManager.new(@organization.dpc_api_organization_id)
    if manager.delete_client_token(id: params[:id])
      flash[:notice] = 'Client token successfully deleted.'
      log_credential_action(:client_token, params[:id], :remove)
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
    render Page::ClientToken::NewTokenComponent.new(@organization, errors: @errors)
  end
end
