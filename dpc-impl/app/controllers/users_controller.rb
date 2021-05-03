# frozen_string_literal: true

class UsersController < ApplicationController

  def email_divert
    @user = find_user

    if confirmation_sent?(@user) && user_unconfirmed?(@user)
      @user.send_confirmation_instructions
    elsif user_invited?(@user) && user_accepted?(@user)
      @user.invite!
    end

    flash[:notice] = email_msg
    redirect_to new_user_session_path
  end

  private

  def confirmation_sent?(user)
    user.confirmation_sent_at.present?
  end

  def email_msg
    return 'If your email address exists in our database, you will receive an email with instructions for how to confirm your email address in a few minutes.'
  end

  def find_user
    user = User.where(email: user_params[:email]).first

    return user
  end

  def user_accepted?(user)
    user.invitation_accepted_at.nil?
  end

  def user_invited?(user)
    user.invitation_sent_at.present?
  end

  def user_unconfirmed?(user)
    user.confirmed_at.nil?
  end

  def user_params
    params.require(:user).permit(:email, :confirmation_sent_at, :confirmed_at, :invitation_accepted_at, :invitation_sent_at )
  end
end
