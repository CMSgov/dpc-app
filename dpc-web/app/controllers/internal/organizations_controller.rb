# frozen_string_literal: true

module Internal
  class OrganizationsController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      scope = Organization.all

      if params[:keyword].present?
        keyword = "%#{params[:keyword].downcase}%"
        scope = scope.where(
          'LOWER(name) LIKE :keyword', keyword: keyword
        )
      end

      @organizations = scope.page params[:page]
      render layout: 'table_index'
    end

    def new
      @organization = Organization.new name: params[:organization_name],
                                       organization_type: params[:organization_organization_type],
                                       num_providers: params[:organization_num_providers]

      @organization.build_address street: params[:organization_address_attributes_street],
                                  street_2: params[:organization_address_attributes_street_2],
                                  city: params[:organization_address_attributes_city],
                                  state: params[:organization_address_attributes_state],
                                  zip: params[:organization_address_attributes_zip]
      @organization.fhir_endpoints.build
    end

    def create
      @organization = Organization.new organization_params
      if @organization.save
        flash[:notice] = 'Organization created.'
        if params[:from_user].present?
          redirect_to edit_internal_user_path(params[:from_user], user_organization_ids: @organization.id)
        else
          redirect_to internal_organization_path(@organization)
        end
      else
        flash[:alert] = "Organization could not be created: #{@organization.errors.full_messages.join(', ')}"
        render :new
      end
    end

    def show
      @organization = Organization.find params[:id]
    end

    def edit
      @organization = Organization.find params[:id]
      @organization.fhir_endpoints.build if @organization.fhir_endpoints.empty?
    end

    def update
      @organization = Organization.find params[:id]
      sandbox_added = (organization_params[:api_environments] - @organization.api_environments).include?('0')

      if @organization.update organization_params
        @organization.notify_users_of_sandbox_access if sandbox_added
        flash[:notice] = 'Organization updated.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization could not be updated: #{@organization.errors.full_messages.join(', ')}"
        render :edit
      end
    end
    def sandbox_added?

    end

    def destroy
      @organization = Organization.find params[:id]
      if @organization.destroy
        flash[:notice] = 'Organization deleted.'
        redirect_to internal_organizations_path
      else
        flash[:alert] = "Organization could not be deleted: #{@organization.errors.full_messages.join(', ')}"
        redirect_to internal_organization_path(@organization)
      end
    end

    private

    def organization_params
      params.fetch(:organization).permit(
        :name, :organization_type, :num_providers, :npi,
        api_environments: [], address_attributes: %i[id street street_2 city state zip address_use address_type],
        fhir_endpoints_attributes: %i[id name status uri]
      )
    end
  end
end
