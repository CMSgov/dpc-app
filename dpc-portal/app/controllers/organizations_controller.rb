# frozen_string_literal: true

# Shows Credential Delegates info about the organizations they manage the credentials for
class OrganizationsController < ApplicationController
  include OrganizationUtils

  before_action :authenticate_user!
  before_action :check_user_verification
  before_action :load_organization, only: %i[show tos_form sign_tos success]
  before_action :require_can_access, only: %i[show]
  before_action :require_ao, only: %i[tos_form sign_tos success]
  before_action :tos_accepted, only: %i[show]

  def index
    @links = current_user.provider_links
    ao_or_cd = @links.any?(AoOrgLink)
    render(Page::Organization::OrganizationListComponent.new(ao_or_cd:, links: @links))
  end

  def show
    @delegate_information = {}
    role = 'Credential Delegate'
    if current_user.ao?(@organization)
      @delegate_information = ao_delegate_information
      role = 'Authorized Official'
    end

    render(Page::Organization::CompoundShowComponent.new(@organization,
                                                         @delegate_information,
                                                         params[:credential_start],
                                                         role,
                                                         cur_org_status))
  end

  def tos_form
    render(Page::Organization::TosFormComponent.new(@organization))
  end

  def sign_tos
    @organization.terms_of_service_accepted_at = DateTime.now
    @organization.terms_of_service_accepted_by = current_user
    @organization.save!
    Rails.logger.info(['Authorized Official signed Terms of Service',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::AoSignedToS }])
    redirect_to organization_path(@organization)
  end

  def success
    render(Page::Organization::NewOrganizationSuccessComponent.new(@organization))
  end

  private

  def organization_id
    params[:id]
  end

  def cur_org_status
    cur_link = current_user.provider_links.find { |link| link.provider_organization_id == @organization.id }
    org_status(@organization, cur_link)
  end

  def ao_delegate_information
    # Invitation expiration is determined in relation to the `created_at` field; the `status` field will
    # never be `'expired'`. Therefore, we need to further filter out expired invitations from this query.
    @delegate_information[:pending] = Invitation.where(provider_organization: @organization,
                                                       invited_by: current_user,
                                                       status: :pending).reject(&:expired?)
    @delegate_information[:expired] = Invitation.where(provider_organization: @organization,
                                                       invited_by: current_user).select(&:expired?)
    @delegate_information[:active] = CdOrgLink.where(provider_organization: @organization, disabled_at: nil)
    @delegate_information
  end
end
