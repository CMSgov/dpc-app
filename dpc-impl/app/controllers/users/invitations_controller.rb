# frozen_string_literal: true

require 'truemail'

module Users
  class InvitationsController < Devise::InvitationsController
    before_action :authenticate_user!
    before_action :configure_permitted_parameters, if: :devise_controller?

    # POST /resource
    def create
      @user = User.new user_params

      if user_valid?(@user)
        @user.invite!(current_user)
        flash[:notice] = 'User invited.'
      else generate_errors(@user)
      end
      redirect_to members_path and return
    end

    private

    def configure_permitted_parameters
      devise_parameter_sanitizer.permit(:accept_invitation) do |u|
        u.permit(:invitation_token, :password, :password_confirmation, :agree_to_terms)
      end
    end

    def generate_errors(user)
      flash[:alert] = 'User was not able to be invited.'

      if !unique_email?(user.email)
        flash[:alert] = 'User already has an account.'
      end

      if !valid_email?(user.email)
        flash[:alert] = 'Email must be valid.'
      end

      if !values_present?(user)
        flash[:alert] = 'All fields are required to invite a new user.'
      end
    end

    def unique_email?(email)
      pre_user = User.where(email: email).first
      pre_user.blank?
    end

    def user_params
      params.require(:user).permit(:first_name, :last_name, :email, :implementer, :implementer_id, :invitation_token, :password, :password_confirmation, :agree_to_terms)
    end

    def user_valid?(user)
      unique_email?(user.email) && valid_email?(user.email) && values_present?(user)
    end
 
    def valid_email?(email)
      result = Truemail.validate(email, with: :mx)
      json = Truemail::Log::Serializer::ValidatorJson.call(result)
      hash = JSON.parse(json)
      return hash["success"]
    end

    def values_present?(user)
      blank_string = ""
      [user.first_name, user.last_name, user.email, user.implementer, user.implementer_id].exclude?(blank_string)
    end
  end
end
