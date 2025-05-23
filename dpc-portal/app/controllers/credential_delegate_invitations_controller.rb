# frozen_string_literal: true

# Manages invitations to become a Credential Delegate
class CredentialDelegateInvitationsController < ApplicationController
  before_action :authenticate_user!
  before_action :check_user_verification
  before_action :load_organization
  before_action :require_ao
  before_action :tos_accepted, except: %i[success destroy]
  before_action :verify_invitation, only: %i[destroy]

  def new
    render(Page::CredentialDelegate::NewInvitationComponent.new(@organization, Invitation.new))
  end

  # rubocop:disable Metrics/AbcSize
  def create
    @cd_invitation = build_invitation

    if @cd_invitation.save
      Rails.logger.info(['Credential Delegate invited',
                         { actionContext: LoggingConstants::ActionContext::Registration,
                           actionType: LoggingConstants::ActionType::CdInvited,
                           invitation: @cd_invitation.id }])
      InvitationMailer.with(invitation: @cd_invitation).invite_cd.deliver_later
      if Rails.env.local?
        logger.info("Invitation URL: #{accept_organization_invitation_url(@organization, @cd_invitation)}")
      end
      flash[:success] = 'Credential Delegate invited successfully.'
      redirect_to organization_path(@organization)
    else
      render(Page::CredentialDelegate::NewInvitationComponent.new(@organization, @cd_invitation), status: :bad_request)
    end
  end
  # rubocop:enable Metrics/AbcSize

  def destroy
    if @invitation.update(status: :cancelled)
      flash[:success] = 'Credential Delegate invitation cancelled successfully.'
    else
      flash[:alert] = destroy_error_message
    end
    redirect_to organization_path(@organization)
  end

  private

  def destroy_error_message
    if @invitation.errors.size == 1 && @invitation.errors.first.type == :cancel_accepted
      @invitation.errors.first.message
    else
      @invitation.errors.full_messages.join(', ')
    end
  end

  def build_invitation
    permitted = params.permit(:invited_given_name, :invited_family_name, :invited_email,
                              :invited_email_confirmation)
    Invitation.new(**permitted.to_h,
                   provider_organization: @organization,
                   invitation_type: :credential_delegate,
                   invited_by: current_user)
  end

  def verify_invitation
    @invitation = Invitation.find(params[:id])
    return if @organization == @invitation.provider_organization

    flash[:alert] = 'You do not have permission to cancel this invitation.'
    redirect_to organization_path(@organization)
  end
end
