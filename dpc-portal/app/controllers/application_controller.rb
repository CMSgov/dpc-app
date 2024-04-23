# frozen_string_literal: true

# Parent class of all controllers
class ApplicationController < ActionController::Base
  before_action :block_prod_sbx
  before_action :check_session_length

  auto_session_timeout User.timeout_in

  private

  def block_prod_sbx
    redirect_to root_url if ENV.fetch('ENV', nil) == 'prod-sbx'
  end

  def check_session_length
    max_session = (User.remember_for * 60).to_i
    return unless max_session.minutes.ago > (session[:logged_in_at] || Time.now)

    reset_session
    flash[:notice] = t('devise.failure.max_session_timeout', default: 'Your session has timed out.')
    redirect_to sign_in_path
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
