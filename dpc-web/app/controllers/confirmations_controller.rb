# frozen_string_literal: true

require './lib/redis_store/mail_throttle_store'

class ConfirmationsController < Devise::ConfirmationsController
  # POST /resource/confirmation
  def create
    mail_throttle_store = RedisStore::MailThrottleStore.new
    if mail_throttle_store.can_email? email_param[:email]
      super
    else
      redirect_to root_path
    end
  end

  private

  def after_confirmation_path_for(_resource_name, resource)
    sign_in(resource) # In case you want to sign in the user
    root_path
  end

  def email_param
    params.require(:user).permit(:email)
  end
end
