# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!
  rescue_from ActiveRecord::RecordNotFound, :with => :unauthorized

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def create
    @organization = current_user.organizations.find(params[:organization_id])
    return render_error('Must have both a label and an API environment.') if missing_invalid_params

    manager = ClientTokenManager.new(api_env: params[:api_environment], organization: @organization)
    if manager.create_client_token(label: params[:label])
      @client_token = manager.client_token
      # @client_token = {'token' => 'MDAxY2xvY2F0aW9uIGh0dHA6Ly9teWJhbmsvCjAwMjZpZGVudGlmaWVyIHdlIHVzZWQgb3VyIHNlY3JldCBrZXkKMDAxNmNpZCB0ZXN0ID0gY2F2ZWF0CjAwMmZzaWduYXR1cmUgGXusegRK8zMyhluSZuJtSTvdZopmDkTYjOGpmMI9vWcK', 'label' => 'Test Token 1', 'createdAt' => Time.now.iso8601}
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
