# frozen_string_literal: true

class DpcRegistrationsController < ApplicationController
  before_action :authenticate_user!

  def show
    @dpc_registration = DpcRegistration.find(params[:id])
  rescue ActiveRecord::RecordNotFound => e
    flash.alert e.message
    redirect_back(fallback_location: root_path)
  end

  def new
    if DpcRegistration.find_by(user: current_user)
      redirect_to dpc_registration_path(current_user.id)
    else
      @dpc_registration = DpcRegistration.new
    end
  end

  def create
    @dpc_registration = DpcRegistration.new(dpc_registrations_params)
    @dpc_registration.user = current_user

    if @dpc_registration.save
      flash[:notice] = 'Registration Successful!'
      redirect_to dpc_registration_path(@dpc_registration)
    else
      render :new
    end
  end

  private

  def dpc_registrations_params
    params.require(:dpc_registration).permit(
      :organization, :address_1, :address_2,
      :city, :state, :zip, :opt_in
    )
  end
end
