# frozen_string_literal: true

# Parent class of all controllers
class ApplicationController < ActionController::Base
  before_action :block_prod_sbx

  private

  def block_prod_sbx
    redirect_to root_url if ENV.fetch('ENV', nil) == 'prod-sbx'
  end

  def organization_id
    params[:organization_id]
  end

  def load_organization
    @organization = ProviderOrganization.find(organization_id)
  rescue ActiveRecord::RecordNotFound
    render file: "#{Rails.root}/public/404.html", layout: false, status: :not_found
  end

  def require_can_access
    redirect_to organizations_path unless current_user.can_access?(@organization)
  end

  def require_ao
    redirect_to organizations_path unless current_user.ao?(@organization)
  end
end
