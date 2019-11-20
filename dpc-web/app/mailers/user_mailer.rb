# frozen_string_literal: true

class UserMailer < ApplicationMailer
  def organization_sandbox_email
    @user = params[:user]
    mail(to: @user.email, subject: 'You have been added to an organization in Data at the Point of Care')
  end
end