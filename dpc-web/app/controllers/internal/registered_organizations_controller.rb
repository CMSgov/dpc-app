# frozen_string_literal: true

module Internal
  class RegisteredOrganizationsController < ApplicationController
    before_action :authenticate_internal_user!

    def new
      @api_env = params[:api_env]
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.build(api_env: @api_env)
      if @api_env == 'sandbox'
        @registered_organization.build_default_fhir_endpoint
      else
        @registered_organization.build_fhir_endpoint
      end
    end

    def create
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations
                                              .build(registered_organization_params)
      @api_env = params[:api_env] || @registered_organization.api_env

      if @registered_organization.save
        api_response = @registered_organization.create_api_organization
        creation_flash_from_api_response(api_response)
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Access to #{@registered_organization.api_env} could not be enabled:
                        #{model_error_string(@registered_organization)}."
        render :new
      end
    end

    def edit
      @api_env = params[:api_env]
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.find(params[:id])
    end

    def update
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.find(params[:id])
      @api_env = @registered_organization.api_env

      if @registered_organization.update(registered_organization_params)
        @registered_organization.update_api_organization
        flash[:notice] = "#{@registered_organization.api_env.capitalize} access updated."
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "#{@registered_organization.api_env.capitalize} access could not be
                        updated: #{model_error_string(@registered_organization)}."
        render :edit
      end
    end

    private

    def creation_flash_from_api_response(api_response)
      if api_response['id'].present?
        flash[:notice] = "Access to #{@registered_organization.api_env} enabled."
      else
        flash[:alert] = "Organization could not be created in the #{@registered_organization.api_env}
                        API: #{api_response}. Edit the organization's environment to try again."
      end
    end

    def registered_organization_params
      params.fetch(:registered_organization).permit(
        :api_env, :organization_id, fhir_endpoint_attributes: %i[id status uri name]
      )
    end
  end
end
