# frozen_string_literal: true

class UsersController < ApplicationController
  before_action :authenticate_admin!

  def index
    @users = User.all.order('created_at DESC')
  end

  def show
    @user = User.find(id_param)
  end

  def update
    @user = User.find(id_param)
    if @user.update user_params
      flash[:notice] = 'User successfully updated.'
    else
      flash[:alert] = "Please correct errors: #{model_error_string(@user)}"
    end
    redirect_to user_url(@user)
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

  private

  def user_params
    params.require(:user).permit(:first_name, :last_name, :email)
  end
end
