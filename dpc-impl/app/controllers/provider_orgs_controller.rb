# frozen_string_literal: true

class ProviderOrgsController < ApplicationController
  before_action :authenticate_user!

  def new
    @user = current_user
  end

  def create
    binding.pry
  end
end
