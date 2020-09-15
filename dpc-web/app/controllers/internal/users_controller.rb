# frozen_string_literal: true

require 'csv'

module Internal
  class UsersController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      results = BaseSearch.new(params: params, scope: params[:org_type]).results

      @users = results.order('users.created_at DESC').page params[:page]
      render layout: 'table_index'
    end

    def show
      @user = User.find(id_param)
    end

    def edit
      @user = User.find(id_param)
      @organizations = Organization.all
    end

    def update
      @user = User.find(id_param)
      @organizations = Organization.all
      if @user.update user_params
        flash[:notice] = 'User successfully updated.'
        redirect_to internal_user_url(@user)
      else
        flash[:alert] = "Please correct errors: #{model_error_string(@user)}"
        render :edit
      end
    end

    def destroy
      @user = User.find(id_param)
      @user.destroy

      if @user.destroy
        flash[:notice] = 'User successfully deleted.'
        redirect_to root_path
      else
        flash[:alert] = 'Unable to delete user.'
      end
    end

    def download
      respond_to do |format|
        filename = "users-#{Time.now.strftime('%Y%m%dT%H%M')}.csv"
        format.csv { send_data User.all.to_csv, filename: filename }
      end
    end

    private

    def user_params
      params.require(:user).permit(:first_name, :last_name, :email, :organization_ids)
    end
  end
end
