# frozen_string_literal: true

class UsersController < ApplicationController
  before_action :authenticate_admin!

  def index
    @users = User.all.order('created_at DESC')
  end

  def show
    @user = User.find(id_param)
  end

  def destroy
    @user = User.find(id_param)
    @user.destroy

    if @user.destroy
      flash[:notice] = 'User successfully deleted.'
      redirect_to users_path
    else
      flash[:alert] = 'Unable to delete user.'
      render user_path(@user)
    end
  end
end
