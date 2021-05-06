# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def index
    @users_accepted = User.invitation_accepted
    @users_pending = User.invitation_not_accepted
  end

  def show
    @user = current_user
  end
end
