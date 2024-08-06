# frozen_string_literal: true

# Emails invitations
class InvitationMailer < ApplicationMailer
  def invite_cd
    @invitation = params[:invitation]
    mail(
      to: @invitation.invited_email,
      subject: 'You have been granted credential delegate authority in Data at the Point of Care'
    )
  end

  def invite_ao
    @invitation = params[:invitation]
    @given_name = params[:given_name]
    @family_name = params[:family_name]
    attachments.inline['dpc.png'] = File.read("#{Rails.root}/app/assets/images/dpc.png")
    mail(
      to: @invitation.invited_email,
      subject: 'Time to register your organization with DPC'
    )
  end
end
