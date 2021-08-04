# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @org_id = org_id
  end

  def create
    @org_id = org_id
    label = params[:label]

    manager = ClientTokenManager.new(imp_id: imp_id, org_id: @org_id)

    if label.present? && manager.create_client_token(label: @label)
      @client_token = manager.client_token
      render :show
    else
      return render_error('Label required.', @org_id) unless label.present?

      render_error 'Client token could not be created.'
    end
  end

  def show
    @client_token = params[:client_token]
  end

  def destroy
    @org_id = org_id
    @token_id = params[:id]

    manager = ClientTokenManager.new(imp_id: imp_id, org_id: @org_id)

    if manager.delete_client_token(id: @token_id)
      flash[:notice] = 'Client token successfully deleted.'
      redirect_to provider_orgs_path(org_id: @org_id)
    else
      render_error 'Client token could not be deleted.'
    end
  end

  private

  def org_id
    params[:org_id]
  end
end