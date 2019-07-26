# frozen_string_literal: true

class DpcRegistrationsController < ApplicationController
  before_action :authenticate_user!

  def new
    DpcRegistration.find_or_create_by(user: current_user)
    redirect_to dpc_registration_path(current_user.id)
  end

  def show
    @dpc_registration = DpcRegistration.find(current_user.id)
  rescue ActiveRecord::RecordNotFound => e
    flash.alert e.message
    redirect_back(fallback_location: root_path)
  end
end
