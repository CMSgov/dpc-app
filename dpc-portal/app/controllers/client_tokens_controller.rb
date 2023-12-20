# frozen_string_literal: true

# Hanles client token requests
class ClientTokensController < ApplicationController
  before_action :load_organization

  def new
    render Page::ClientToken::NewTokenComponent.new(@organization)
  end

  def create
    manager = ClientTokenManager.new(params[:organization_id])
    if params_present? && manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      render(Page::ClientToken::ShowTokenComponent.new(@organization, @client_token))
    else
      return render_error 'Label required.' unless params_present?

      render_error 'Client token could not be created.'
    end
  end

  def destroy
    manager = ClientTokenManager.new(params[:organization_id])
    if manager.delete_client_token(id: params[:id])
      flash[:notice] = 'Client token successfully deleted.'
    else
      flash[:alert] = 'Client token could not be deleted.'
    end
    redirect_to organization_path(params[:organization_id])
  end

  private

  def params_present?
    params[:label].present?
  end

  def render_error(msg)
    flash.now.alert = msg
    render Page::ClientToken::NewTokenComponent.new(@organization)
  end

  def load_organization
    @organization = case ENV.fetch('ENV', nil)
                    when 'prod-sbx'
                      redirect_to root_url
                    when 'test'
                      Organization.new('6a1dbf47-825b-40f3-b81d-4a7ffbbdc270')
                    when 'dev'
                      Organization.new('78d02106-2837-4d07-8c51-8d73332aff09')
                    else
                      Organization.new(params[:organization_id])
                    end
  end
end
