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
      @organization = Organization.new
      @organization.build_address
    end

    def create
      @organization = Organization.new organization_params
      if @organization.save
        flash[:notice] = 'Organization created.'
        redirect_to internal_organization_path(@organization)
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
    end

    def update
      @organization = Organization.find params[:id]
      if @organization.update organization_params
        flash[:notice] = 'Organization updated.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization could not be updated: #{@organization.errors.full_messages.join(', ')}"
        render :edit
      end
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
        :name, :organization_type, :num_providers,
        address_attributes: %i[id street street_2 city state zip]
      )
    end
  end
end
