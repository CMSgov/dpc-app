# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
    @client_tokens = @user.primary_organization.reg_org.client_tokens()
  end
end
