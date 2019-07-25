# frozen_string_literal: true

class DpcRegistrationsController < ApplicationController
  before_action :authenticate_user!

  def show
    byebug
    @dpc_registration = DpcRegistration.find(current_user.id)
  rescue ActiveRecord::RecordNotFound => e
    flash.alert e.message
    redirect_back(fallback_location: root_path)
  end
end
