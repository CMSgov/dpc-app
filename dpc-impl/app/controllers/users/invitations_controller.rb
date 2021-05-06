# frozen_string_literal: true

require 'truemail'

module Users
  class InvitationsController < Devise::InvitationsController
    before_action :authenticate_user!
    before_action :configure_permitted_parameters, if: :devise_controller?

    # POST /resource
    def create
      @user = User.new user_params

      if values_present?(@user) && valid_email?(@user.email) && unique_email?(@user.email)
        @user.invite!(current_user)
        flash[:notice] = 'User invited.'
        redirect_to root_path
      elsif !values_present?(@user)
        flash[:alert] = 'All fields are required to invite a new user.'
        redirect_to new_user_invitation_path
      elsif !valid_email?(@user.email)
        flash[:alert] = 'Email must be valid.'
        redirect_to new_user_invitation_path
      elsif !unique_email?(@user.email)
        flash[:alert] = 'Email already exists in DPC.'
        redirect_to new_user_invitation_path
      else
        flash[:alert] = 'User was not able to be invited.'
        redirect_to root_path
      end
    end

    private

    def configure_permitted_parameters
      devise_parameter_sanitizer.permit(:accept_invitation) do |u|
        u.permit(:invitation_token, :password, :password_confirmation, :agree_to_terms)
      end
    end

    def user_params
      params.require(:user).permit(:first_name, :last_name, :email, :implementer, :implementer_id, :invitation_token, :password, :password_confirmation, :agree_to_terms)
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

    def unique_email?(email)
      pre_user = User.where(email: email).first
      pre_user.blank?
    end
  end
end
