# frozen_string_literal: true

class UsersController < ApplicationController
  before_action :authenticate_admin!

  def index
    @users = User.all
  end
end
