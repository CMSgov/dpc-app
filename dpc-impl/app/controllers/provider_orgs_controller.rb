# frozen_string_literal: true

class ProviderOrgsController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
  end

  private
end
