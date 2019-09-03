class Internal::UsersController < ApplicationController
  before_action :authenticate_internal_user!
  def index
  end
end