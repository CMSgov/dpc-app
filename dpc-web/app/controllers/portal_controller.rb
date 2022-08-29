# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
    if current_user.unassigned? || current_user.primary_organization.reg_org.nil?
      @client_tokens = []
      @public_keys = []
    else
      if params.has_key?(:page)
        @client_tokens = Kaminari.paginate_array(@user.primary_organization.reg_org.client_tokens).page(params[:page]).per(5)
        @public_keys = Kaminari.paginate_array(@user.primary_organization.reg_org.public_keys).page(params[:page]).per(5)
      else
        @client_tokens = Kaminari.paginate_array(@user.primary_organization.reg_org.client_tokens).page(0).per(5)
        @public_keys = Kaminari.paginate_array(@user.primary_organization.reg_org.public_keys).page(0).per(5)
      end
    end
  end
end
