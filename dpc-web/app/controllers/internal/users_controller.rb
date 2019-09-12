# frozen_string_literal: true

module Internal
  class UsersController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      @users = User.order('created_at DESC').page params[:page]
    end

    def show
      @user = User.find(params[:id])
    end
  end
end
