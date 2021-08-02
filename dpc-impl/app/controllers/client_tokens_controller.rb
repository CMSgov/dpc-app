# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @org_id = org_id
  end

  def create
    @org_id = org_id
    @label = params[:label]

    manager = ClientTokenManager.new(imp_id: imp_id, org_id: @org_id)

    if params_present?(@label) && manager.create_client_token(label: @label)
      @client_token = manager.client_token
      render :show
    else
      return render_error 'Label required.' unless params_present?

      render_error 'Client token could not be created.'
    end
  end

  def destroy
  end

  private

  def get_client_token(manager)
    instance_variables.each do |ivar_name|
      binding.pry
      if instance_variable_get(ivar_name) == manager
        return ivar_name.to_s.sub(/^@/, '') # change '@something' to 'something'
      end
    end
  
    # return nil if no match was found
    nil 
  end

  def org_id
    params[:org_id]
  end

  def params_present?(label)
    label.present?
  end
end