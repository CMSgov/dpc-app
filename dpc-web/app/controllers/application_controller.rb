# frozen_string_literal: true

class ApplicationController < ActionController::Base
  protect_from_forgery
  before_action :configure_permitted_parameters, if: :devise_controller?

  def configure_permitted_parameters
    devise_parameter_sanitizer.permit(:sign_up) do |user|
      user.permit(
        :first_name, :last_name, :organization,
        :address_1, :address_2, :city, :state, :zip,
        :email, :password
      )
    end

    devise_parameter_sanitizer.permit(:account_update) do |user|
      user.permit(
        :first_name, :last_name,  :organization,
        :address_1, :address_2, :city, :state, :zip,
        :email, :password, :current_password
      )
    end
  end
end
