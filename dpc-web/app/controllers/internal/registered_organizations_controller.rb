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
      @registered_organization.enabled = true

      if @registered_organization.save
        flash[:notice] = 'API has been enabled.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "API could not be enabled:
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
        flash[:notice] = 'Organization successfully updated in API.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization could not be
                        updated: #{model_error_string(@registered_organization)}."
        render :edit
      end
    end

    def destroy
      @organization = Organization.find(org_id_param)
      @registered_organization = @organization.registered_organization

      if @registered_organization.destroy
        flash[:notice] = 'API access disabled.'
      else
        flash[:alert] = "API access could not be
                        disabled: #{model_error_string(@registered_organization)}."
      end
      redirect_to internal_organization_path(@organization)
    end

    def enable_or_disable
      @organization = Organization.find(org_id_param)
      @reg_org = @organization.registered_organization

      if @reg_org.enabled == true
        @reg_org.enabled = false
        @reg_org.save

        flash[:notice] = 'API access disabled.'
        redirect_to internal_organization_path(@organization)
      else @reg_org.enabled == false
        @reg_org.enabled = true
        @reg_org.save

        flash[:notice] = 'API access enabled.'
        redirect_to internal_organization_path(@organization)
      end
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
