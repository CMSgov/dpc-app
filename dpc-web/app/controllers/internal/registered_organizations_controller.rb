# frozen_string_literal: true

module Internal
  class RegisteredOrganizationsController < ApplicationController
    before_action :authenticate_internal_user!

    def new
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.build(api_env: params[:api_env])
      if params[:api_env] == 'sandbox'
        @registered_organization.build_default_fhir_endpoint
      else
        @registered_organization.build_fhir_endpoint
      end
    end

    def create
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.
        build(registered_organization_params)
      if @registered_organization.save
        flash[:notice] = "Access to #{@registered_organization.api_env} enabled."
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Access to #{@registered_organization.api_env} could not be enabled: #{model_error_string(@registered_organization)}."
        render :new
      end
    end

    def edit
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.find(params[:id])
    end

    def update
      @organization = Organization.find(params[:organization_id])
      @registered_organization = @organization.registered_organizations.find(params[:id])
      if @registered_organization.update(registered_organization_params)
        flash[:notice] = "#{@registered_organization.api_env} access updated."
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] =
          "#{@registered_organization.api_env} access could not be updated: #{model_error_string(@registered_organization)}."
        render :edit
      end
    end

    private

    def registered_organization_params
      params.fetch(:registered_organization).permit(
        :api_env, :organization_id, fhir_endpoint_attributes: %i[id status uri]
      )
    end
  end
end