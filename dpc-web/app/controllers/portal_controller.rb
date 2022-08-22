# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
    if current_user.unassigned? || current_user.primary_organization.nil?
      @client_tokens = []
      @public_keys = []
    else
      @client_tokens = @user.primary_organization.reg_org.client_tokens
      @public_keys = @user.primary_organization.reg_org.public_keys
    end
  end
end
