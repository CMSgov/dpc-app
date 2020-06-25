# frozen_string_literal: true

module Internal
  class RegisteredOrganizationsController < ApplicationController
    before_action :authenticate_internal_user!

    def new
      @organization = Organization.find(org_id_param)
      @registered_organization = @organization.build_registered_organization
      if prod_sbx?
        @registered_organization.build_default_fhir_endpoint
      else
        @registered_organization.build_fhir_endpoint
      end
    end

    def create
      @organization = Organization.find(org_id_param)
      @registered_organization = @organization.build_registered_organization(registered_organization_params)

      if @registered_organization.save
        flash[:notice] = 'Organization has been enabled.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization could not be enabled:
                        #{model_error_string(@registered_organization)}."
        render :new
      end
    end

    def edit
      @organization = Organization.find(org_id_param)
      @registered_organization = @organization.registered_organization
    end

    def update
      @organization = Organization.find(org_id_param)
      @registered_organization = @organization.registered_organization

      if @registered_organization.update(registered_organization_params)
        flash[:notice] = 'Organization access updated.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization access could not be
                        updated: #{model_error_string(@registered_organization)}."
        render :edit
      end
    end

    def destroy
      @organization = Organization.find(org_id_param)
      @registered_organization = @organization.registered_organization

      if @registered_organization.destroy
        flash[:notice] = 'Organization access disabled.'
      else
        flash[:alert] = "Organization access could not be
                        disabled: #{model_error_string(@registered_organization)}."
      end
      redirect_to internal_organization_path(@organization)
    end

    private

    def org_id_param
      params.require(:organization_id)
    end

    def registered_organization_params
      params.fetch(:registered_organization).permit(
        :organization_id, fhir_endpoint_attributes: %i[id status uri name]
      )
    end
  end
end
