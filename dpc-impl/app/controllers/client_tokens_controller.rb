# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = org_id
  end

  def create
    @org_id = org_id

    api_client = ApiClient.new


    if api_client.create_client_token(imp_id, org_id, 
                                      label: params[:label])
      binding.pry
    else
      render_error('Client Token could not be created', @org_id)
    end
  end

  def destroy
  end

  private

  def org_id
    params[:org_id]
  end
end