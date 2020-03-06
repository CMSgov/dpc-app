# frozen_string_literal: true

module Users
  class RegistrationsController < Devise::RegistrationsController
    include MultiModelLoginHelper
    skip_before_action :check_user, except: %i[new create]

    def destroy
      @user = User.find(current_user.id)
      if @user.destroy_with_password(user_params[:password_to_delete])
          redirect_to root_url, notice: "User deleted."
      else
        redirect_to edit_user_registration_url
        flash[:notice] = "Couldn't delete"
      end
    end

    def user_params
      params.require(:user).permit(:first_name, :last_name, :requested_organization, :requested_organization_type,
        :address_1, :address_2, :city, :state, :zip, :agree_to_terms,
        :email, :password, :password_confirmation, :current_password, :requested_num_providers, :password_to_delete )
    end
    # before_action :configure_sign_up_params, only: [:create]
    # before_action :configure_account_update_params, only: [:update]

    # GET /resource/sign_up
    # def new
    #   super
    # end

    # POST /resource
    # def create
    #   super
    # end

    # GET /resource/edit
    # def edit
    #   super
    # end

    # PUT /resource
    # def update
    #   super
    # end

    # DELETE /resource
    # def destroy
    #   super
    # end

    # GET /resource/cancel
    # Forces the session data which is usually expired after sign
    # in to be expired now. This is useful if the user wants to
    # cancel oauth signing in/up in the middle of the process,
    # removing all OAuth session data.
    # def cancel
    #   super
    # end

    # protected

    # If you have extra params to permit, append them to the sanitizer.
    # def configure_sign_up_params
    #   devise_parameter_sanitizer.permit(:sign_up, keys: [:attribute])
    # end

    # If you have extra params to permit, append them to the sanitizer.
    # def configure_account_update_params
    #   devise_parameter_sanitizer.permit(:account_update, keys: [:attribute])
    # end

    # The path used after sign up.
    # def after_sign_up_path_for(resource)
    #   super(resource)
    # end

    # The path used after sign up for inactive accounts.
    # def after_inactive_sign_up_path_for(resource)
    #   super(resource)
    # end
  end
end
