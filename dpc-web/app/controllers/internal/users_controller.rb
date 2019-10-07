# frozen_string_literal: true

require 'csv'

module Internal
  class UsersController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      scope = User.all

      if params[:keyword].present?
        keyword = "%#{params[:keyword].downcase}%"
        scope = scope.where(
          'LOWER(first_name) LIKE :keyword OR LOWER(last_name) LIKE :keyword OR LOWER(email) LIKE :keyword',
          keyword: keyword
        )
      end

      if params[:requested_org].present?
        org = "%#{params[:requested_org].downcase}%"
        scope = scope.where('LOWER(requested_organization) LIKE :org', org: org)
      end

      if params[:requested_org_type].present?
        scope = scope.where(requested_organization_type: params[:requested_org_type])
      end

      if params[:created_after].present?
        scope = scope.where('created_at > :created_after', created_after: params[:created_after])
      end

      if params[:created_before].present?
        scope = scope.where('created_at < :created_before', created_before: params[:created_before])
      end

      @users = scope.order('created_at DESC').page params[:page]
    end

    def show
      @user = User.find(params[:id])
    end

    def edit
      @user = User.find(params[:id])
    end

    def update
      @user = User.find(params[:id])
      if @user.update user_params
        flash[:notice] = 'User successfully updated.'
        redirect_to internal_user_url(@user)
      else
        flash[:alert] = "Please correct errrors: #{@user.errors.full_messages.join(', ')}"
        render :edit
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
      params.fetch(:user).permit(:first_name, :last_name, :email)
    end
  end
end
