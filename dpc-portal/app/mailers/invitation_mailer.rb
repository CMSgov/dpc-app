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
end
