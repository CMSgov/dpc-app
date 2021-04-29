# frozen_string_literal: true

module Users
  class InvitationsController < Devise::InvitationsController
    before_action :authenticate_user!

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
      params.require(:user).permit(:first_name, :last_name, :email, :implementer)
    end
  end
end
