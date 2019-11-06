# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  rescue ActiveRecord::RecordNotFound
    flash[:error] = 'Unauthorized'
    redirect_to dashboard_path
  end

  def create
    return render_error('Must have both a label and an API environment.') if missing_invalid_params

    @organization = current_user.organizations.find(params[:organization_id])
    manager = ClientTokenManager.new(api_env: params[:api_environment], organization: @organization)

    if manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      render :show
    else
      render_error 'Client token could not be created.'
    end

  rescue ActiveRecord::RecordNotFound
    flash[:error] = 'Unauthorized'
    redirect_to dashboard_path
  end

  # Need to get client token api env
  # probably need to store client tokens in this DB so we can get list and queue up actions if the API is down
  # and we'll know envs without making a call
  # but if the token gets deleted on the API without going through this UI, this DB will need to stay up to date
  def destroy
    APIClient.new(api_env).delete_client_token(params[:id])
  end

  private

  def render_error(msg)
    flash[:error] = msg
    render :new
  end

  def missing_invalid_params
    params[:api_environment].blank? || params[:label].blank?
  end
end
