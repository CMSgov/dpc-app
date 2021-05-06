# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def index
    @users_accepted = users_accepted
    @users_pending = users_pending
  end

  def show
    @user = current_user
  end

  private

  def current_user_imp_id
    current_user.implementer_id
  end

  def users_accepted
    User.invitation_accepted.where(implementer_id: current_user_imp_id)
  end

  def users_pending
    User.invitation_not_accepted.where(implementer_id: current_user_imp_id)
  end
end
