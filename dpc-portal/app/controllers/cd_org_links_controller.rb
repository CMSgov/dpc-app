# frozen_string_literal: true

# Handles deletion of CdOrgLinks
class CdOrgLinksController < ApplicationController
  before_action :authenticate_user!
  before_action :verify_ao_for_organization

  def destroy
    cd_org_link.destroy!
    log_event(:info, 'Credential Delegate removed from organization',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::CdRemovedFromOrg,
              **csp_log_context)
    flash[:success] = 'Successfully removed Credential Delegate.'
  rescue ActiveRecord::RecordNotDestroyed
    log_event(:error, 'Credential Delegate not removed from organization',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::CdNotRemovedFromOrg,
              **csp_log_context)
    flash[:alert] = 'Failed to remove Credential Delegate. Please try again later.'
  ensure
    redirect_to organization_path(organization)
  end

  private

  def organization
    @organization ||= ProviderOrganization.find(params[:organization_id])
  end

  def cd_org_link
    @cd_org_link ||= organization.cd_org_links.find(params[:id])
  end

  def verify_ao_for_organization
    return if current_user.ao?(organization)

    redirect_to organization_path(organization)
  end
end
