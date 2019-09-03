class Internal::UsersController < ApplicationController
  before_action :authenticate_internal_user!
  def index
    @users = User.all
  end
end