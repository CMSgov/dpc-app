# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def index
    @users_accepted = User.invitation_accepted.where(implementer_id: current_user.implementer_id)
    @users_pending = User.invitation_not_accepted.where(implementer_id: current_user.implementer_id)
  end

  def show
    @user = current_user
  end
end
