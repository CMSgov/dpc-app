# frozen_string_literal: true

# Emails invitations
class InvitationMailer < ApplicationMailer
  def invite_cd
    @invitation = params[:invitation]
    mail(
      to: @invitation.invited_email,
      subject: 'Action required: You’ve been delegated to manage API access'
    )
  end

  def invite_ao
    @invitation = params[:invitation]
    @given_name = params[:given_name]
    @family_name = params[:family_name]
    mail(
      to: @invitation.invited_email,
      subject: 'Action required: Register your organization to continue accessing API claims data'
    )
  end

  def cd_accepted
    @invitation = params[:invitation]
    @invited_given_name = params[:invited_given_name]
    @invited_family_name = params[:invited_family_name]
    mail(
      to: @invitation&.invited_by&.email,
      subject: 'Credential Delegate has signed up successfully'
    )
  end
end
