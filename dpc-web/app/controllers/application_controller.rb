# frozen_string_literal: true

class ApplicationController < ActionController::Base
  protect_from_forgery
  before_action :configure_permitted_parameters, if: :devise_controller?
  before_action :load_markdown

  def configure_permitted_parameters
    devise_parameter_sanitizer.permit(:sign_up) do |user|
      user.permit(
        :first_name, :last_name, :organization, :organization_type,
        :address_1, :address_2, :city, :state, :zip, :agree_to_terms,
        :email, :password, :current_password
      )
    end

    devise_parameter_sanitizer.permit(:account_update) do |user|
      user.permit(
        :first_name, :last_name, :organization, :organization_type,
        :address_1, :address_2, :city, :state, :zip, :agree_to_terms,
        :email, :password, :current_password
      )
    end
  end

  protected

  def load_markdown
    # Assume markdown is named the same as the action.
    file_path = lookup_context.find_template("#{controller_path}/#{action_name}").identifier.sub('.html.erb', '.md')
    if File.exist?(file_path)
      md_content = File.read(file_path)
      @html_content = Kramdown::Document.new(md_content).to_html
    end
  end
end
