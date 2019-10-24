# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def create
    redirect_if_invalid
    # Kick off token creation request to API
    @client_token = APIClient.new(params[:api_environment]).create_client_token

    if @client_token
      render :show
    else
      flash[:error] = 'Client token could not be created.'
      render :new
    end
  end

  # Need to get client token api env
  # probably need to store client tokens in this DB so we can get list and queue up actions if the API is down
  # and we'll know envs without making a call
  # but if the token gets deleted on the API without going through this UI, this DB will need to stay up to date
  def destroy
    APIClient.new(api_env).delete_client_token(params[:id])
  end

  private

  def redirect_if_invalid
    return render :new if params[:api_environment].blank? or params[:description].blank?
  end

  def client_token_params
    params.permit(:description, :api_environment)
  end
end
