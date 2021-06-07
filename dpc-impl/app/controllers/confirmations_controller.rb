# frozen_string_literal: true

require './lib/redis_store/mail_throttle_store'

class ConfirmationsController < Devise::ConfirmationsController

  # POST /resource/confirmation
  def create
    @user = find_user

    flash[:notice] = email_msg

    # Resends correct email if user needs to confirm email or accept invitation
    if @user.present?
      if confirmation_sent?(@user) && user_unconfirmed?(@user)
        send_confirmation_instructions(@user)
      elsif user_invited?(@user) && user_accepted?(@user)
        send_user_invite(@user)
      end
    end

    redirect_to new_user_session_path
  end

  private

  def after_confirmation_path_for(_resource_name, resource)
    sign_in(resource) # In case you want to sign in the user
    portal_path
  end

  def confirmation_sent?(user)
    user.confirmation_sent_at.present?
  end

  def email_msg
    return 'If your email address exists in our database, you will receive an email with instructions for how to confirm your email address in a few minutes.'
  end

  def find_user
    User.where(email: user_email).first
  end

  def mail_throttle_store
    RedisStore::MailThrottleStore.new
  end

  def send_confirmation_instructions(user)
    if mail_throttle_store.can_email? user_email
      user.send_confirmation_instructions
    else
      return
    end
  end

  def send_user_invite(user)
    if mail_throttle_store.can_email? user_email
      user.invite!
    else
      return
    end
  end

  def user_accepted?(user)
    user.invitation_accepted_at.nil?
  end

  def user_email
    user_params[:email]
  end

  def user_invited?(user)
    user.invitation_sent_at.present?
  end

  def user_unconfirmed?(user)
    user.confirmed_at.nil?
  end

  def user_params
    params.require(:user).permit(:email, :confirmation_sent_at, :confirmed_at, :invitation_accepted_at, 
                                 :invitation_sent_at )
  end
end
