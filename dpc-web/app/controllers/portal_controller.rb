# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
    if current_user.unassigned? || current_user.primary_organization.reg_org.nil?
      @client_tokens = []
      @public_keys = []
    else
      @public_keys = if params.key?(:public_key)
        Kaminari.paginate_array(@user.primary_organization.reg_org.public_keys).page(params[:key_page]).per(10)
      else
        Kaminari.paginate_array(@user.primary_organization.reg_org.public_keys).page(0).per(10)
      end

      @client_tokens = if params.key?(:client_token)
        Kaminari.paginate_array(@user.primary_organization.reg_org.client_tokens).page(params[:token_page]).per(10)
      else
        Kaminari.paginate_array(@user.primary_organization.reg_org.client_tokens).page(0).per(10)
      end

    end
  end
end
