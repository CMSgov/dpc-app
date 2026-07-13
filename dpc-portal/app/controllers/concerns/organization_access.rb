# frozen_string_literal: true

# Handles organization access for application controllers
module OrganizationAccess
  extend ActiveSupport::Concern

  def require_can_access
    redirect_to organizations_path unless current_user.can_access?(@organization)

    verify_status
  end

  def require_ao
    redirect_to organizations_path unless current_user.ao?(@organization)

    verify_status
  end

  def tos_accepted
    return if @organization.terms_of_service_accepted_by.present?

    if current_user.ao?(@organization)
      render(Page::Organization::TosFormComponent.new(@organization))
    else
      flash[:notice] = 'Organization is not ready for credential management'
      redirect_to organizations_path
    end
  end

  private

  def organization_id
    params[:organization_id]
  end

  def code_prefix
    has_ao_link = current_user.ao_org_links.where(provider_organization: @organization).exists?
    has_ao_link ? 'verification' : 'cd_access'
  end

  def verify_status
    if @organization.rejected?
      failure_code = "#{code_prefix}.#{@organization.verification_reason}"
      return render(Page::Utility::AccessDeniedComponent.new(organization: @organization, failure_code:))
    end

    links = current_user.ao_org_links.where(provider_organization: @organization)
    return if links.empty? || links.any?(&:verification_status?)

    failure_code = "verification.#{links.first.verification_reason}"
    render(Page::Utility::AccessDeniedComponent.new(organization: @organization, failure_code:))
  end

  def load_organization
    @organization = ProviderOrganization.find(organization_id)
    CurrentAttributes.save_organization_attributes(@organization, current_user)
  rescue ActiveRecord::RecordNotFound
    render file: "#{Rails.root}/public/404.html", layout: false, status: :not_found
  end
end
