# frozen_string_literal: true

module Users
  class InvitationsController < Devise::InvitationsController
    before_action :authenticate_user!
    before_action :configure_permitted_parameters, if: :devise_controller?

    # POST /resource
    def create
      @user = User.new user_params
      if @user.invite!
        flash[:notice] = 'User invited.'
      else
        flash[:alert] = 'User could not be invited.'
      end
      redirect_to root_path
    end

    private

    def user_params
      params.require(:user).permit(:first_name, :last_name, :email, :implementer, :invitation_token, :password, :password_confirmation, :agree_to_terms)
    end

    def configure_permitted_parameters
      devise_parameter_sanitizer.permit(:accept_invitation) do |u|
        u.permit(:invitation_token, :password, :password_confirmation, :agree_to_terms)
      end
    end
  end
end
