# frozen_string_literal: true

module Internal
  class OrganizationsController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      results = BaseSearch.new(params: params, scope: params[:org_type]).results

      @organizations = org_page_params(results)
      render layout: 'table_index'
    end

    def new
      if from_user_params[:from_user].present?
        user = User.find from_user_params[:from_user]
        @organization = Organization.new name: user.requested_organization,
                                         organization_type: user.requested_organization_type,
                                         num_providers: user.requested_num_providers

        @organization.build_address street: user.address_1,
                                    street_2: user.address_2,
                                    city: user.city,
                                    state: user.state,
                                    zip: user.zip
      else
        @organization = Organization.new
        @organization.build_address
      end
    end

    def create
      @organization = Organization.new organization_params

      if @organization.save
        flash[:notice] = 'Organization created.'

        add_user(from_user_params[:from_user]) if from_user_params[:from_user].present?

        if prod_sbx?
          redirect_to new_internal_organization_registered_organization_path(organization_id: @organization.id)
        elsif from_user_params[:from_user].present?
          redirect_to edit_internal_user_path(from_user_params[:from_user], user_organization_ids: @organization.id)
        else
          redirect_to internal_organization_path(@organization)
        end
      else
        flash[:alert] = "Organization could not be created: #{model_error_string(@organization)}"
        render :new
      end
    end

    def show
      @organization = Organization.find id_param

      @users = user_filter
    end

    def edit
      @organization = Organization.find id_param
    end

    def update
      @organization = Organization.find id_param

      if @organization.update organization_params
        flash[:notice] = 'Organization updated.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization could not be updated: #{model_error_string(@organization)}"
        render :edit
      end
    end

    def destroy
      @organization = Organization.find id_param
      if @organization.destroy
        flash[:notice] = 'Organization deleted.'
        redirect_to internal_organizations_path
      else
        flash[:alert] = "Organization could not be deleted: #{model_error_string(@organization)}"
        redirect_to internal_organization_path(@organization)
      end
    end

    def add_or_delete
      @organization = Organization.find(params[:organization_id])
      @user = user_identify

      add_delete(params)
    end

    private

    def add_delete(params)
      if params[:_method] == 'add'
        @user.organizations.clear
        add_action = @organization.users << @user
        action = 'added to'
      elsif params[:_method] == 'delete'
        delete_action = @organization.users.delete(@user)
        action = 'deleted from'
      end

      if add_action || delete_action
        flash[:notice] = "User has been successfully #{action} the organization."
        page_redirect
      else
        flash[:alert] = "User could not be #{action} the organization ."
      end
    end

    def org_page_params(results)
      results.page params[:page]
    end

    def page_redirect
      return redirect_to internal_user_url(@user) if params[:user_id].present?

      redirect_to internal_organization_path(@organization)
    end

    def from_user_params
      params.permit(:from_user)
    end

    def user_filter
      User.left_joins(:organization_user_assignments)
          .where('organization_user_assignments.id IS NULL')
    end

    def user_identify
      return User.find(params[:user_id]) if params[:user_id].present?

      User.find(params[:organization][:id])
    end

    def organization_params
      params.require(:organization).permit(
        :name, :organization_type, :num_providers, :npi, :vendor,
        address_attributes: %i[id street street_2 city state zip address_use address_type]
      )
    end
  end
end
