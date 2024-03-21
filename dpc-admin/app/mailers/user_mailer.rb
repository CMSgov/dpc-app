# frozen_string_literal: true

class UserMailer < ApplicationMailer
  def organization_sandbox_email
    template_name = params[:vendor] ? 'vendor_sandbox_email' : 'provider_sandbox_email'

    @user = params[:user]
    mail(
      to: @user.email,
      subject: 'You have been added to an organization in Data at the Point of Care',
      template_path: 'user_mailer',
      template_name:
    )
  end
end
