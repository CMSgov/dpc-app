# frozen_string_literal: true

require './lib/redis_store/mail_throttle_store'

module Users
  class PasswordsController < Devise::PasswordsController
    # GET /resource/password/new
    # def new
    #   super
    # end

    # POST /resource/password
    def create
      mail_throttle_store = RedisStore::MailThrottleStore.new
      if mail_throttle_store.can_email? email_param[:email]
        super
      else
        redirect_to root_path
      end
    end

    # GET /resource/password/edit?reset_password_token=abcdef
    # def edit
    #   super
    # end

    # PUT /resource/password
    # def update
    #   super
    # end

    # protected

    # def after_resetting_password_path_for(resource)
    #   super(resource)
    # end

    # The path used after sending reset password instructions
    # def after_sending_reset_password_instructions_path_for(resource_name)
    #   super(resource_name)
    # end

    private

    def email_param
      params.require(:user).permit(:email)
    end
  end
end
