# frozen_string_literal: true

class ApplicationController < ActionController::Base
  protect_from_forgery
  before_action :configure_permitted_parameters, if: :devise_controller?

  def configure_permitted_parameters
    devise_parameter_sanitizer.permit(:sign_up) do |user|
      user.permit(
        :first_name, :last_name, :requested_organization, :requested_organization_type,
        :address_1, :address_2, :city, :state, :zip, :agree_to_terms,
        :email, :password, :password_confirmation, :current_password, :requested_num_providers
      )
    end

    devise_parameter_sanitizer.permit(:account_update) do |user|
      user.permit(
        :first_name, :last_name, :requested_organization, :requested_organization_type,
        :address_1, :address_2, :city, :state, :zip, :agree_to_terms,
        :email, :password, :password_confirmation, :current_password, :requested_num_providers
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

  def download_jwt_tool
    send_file 'public/jwt/jwt.html', type: 'application/zip', status: 202
  end

  def download_prac_json
    send_file 'public/practitioner_bundle.json', type: 'application/zip', status: 202
  end

  def download_pt_json
    send_file 'public/patient_bundle.json', type: 'application/zip', status: 202
  end

  private

  def id_param
    params.require(:id)
  end

  def prod_sbx?
    ENV['ENV'] == 'prod-sbx'
  end
  helper_method :prod_sbx?
end
