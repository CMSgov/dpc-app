# frozen_string_literal: true

require './lib/luhnacy_lib/luhnacy_lib'

class ApplicationController < ActionController::Base
  protect_from_forgery
  before_action :configure_permitted_parameters, if: :devise_controller?

  def configure_permitted_parameters
    devise_parameter_sanitizer.permit(:sign_up) do |user|
      user.permit(
        :first_name, :last_name, :email, :implementer,
        :password, :password_confirmation, :current_password, :agree_to_terms, :invitation_sent_at
      )
    end

    devise_parameter_sanitizer.permit(:account_update) do |user|
      user.permit(
        :first_name, :last_name, :email, :implementer,
        :password, :password_confirmation, :current_password, :agree_to_terms, :invitation_sent_at
      )
    end
  end

  def model_error_string(resource)
    resource.errors.full_messages.join(', ')
  end

  # For increased logging on errors
  def append_info_to_payload(payload)
    super
    payload[:level] = if payload[:status] == 200
                        'INFO'
                      elsif payload[:status] == 302
                        'WARN'
                      else
                        'ERROR'
                      end
  end

  def after_sign_out_path_for(resource)
    request.referrer
  end

  private

  def generate_npi
    LuhnacyLib.generate_npi
  end

  def id_param
    params.require(:id)
  end

  def imp_id
    current_user.implementer_id
  end

  def api_service
    @api_service ||= ApiClient.new
  end

  def missing_key
    params[:public_key].blank?
  end

  def render_error(msg, org)
    flash[:alert] = msg
    redirect_to provider_orgs_path(org_id: org)
  end

  def user_params
    params.require(:user).permit(:first_name, :last_name, :email, :implementer, :implementer_id, :invitation_token, :password, :password_confirmation, :agree_to_terms)
  end
end
