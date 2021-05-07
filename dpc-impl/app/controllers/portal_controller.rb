# frozen_string_literal: true

class PortalController < ApplicationController
  before_action :authenticate_user!

  def index
    @user = current_user
    @first_user = first_user
    @users_accepted = users_accepted
    @users_pending = users_pending

    resource = :users
  end

  def show
    @user = current_user
  end

  private

  def current_user_imp_id
    current_user.implementer_id
  end

  def first_user
    User.where(implementer_id: current_user_imp_id, invitation_sent_at: nil).first
  end

  def users_accepted
    User.invitation_accepted.where(implementer_id: current_user_imp_id)
  end

  def users_pending
    User.invitation_not_accepted.where(implementer_id: current_user_imp_id)
  end

  # for devise invitable

  def resource_name
    :user
  end
  helper_method :resource_name

  def resource
    @resource ||= User.new
  end
  helper_method :resource

  def devise_mapping
    @devise_mapping ||= Devise.mappings[:user]
  end
  helper_method :devise_mapping

  def resource_class
    User
  end
  helper_method :resource_class
end
