# frozen_string_literal: true

class OrganizationsController < ApplicationController
  before_action :authenticate_internal_user!

  def index
    results = BaseSearch.new(params:, scope: params[:org_type]).results
    @tags = Tag.all

    @organizations = org_page_params(results)
    render layout: 'table_index'
  end

  def new
    if user_id_params[:user_id].present?
      user = User.find user_id_params[:user_id]
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

      if user_id_params[:user_id].present?
        @user = User.find user_id_params[:user_id]
        add_user_to_org
        return
      end

      if prod_sbx?
        redirect_to new_organization_registered_organization_path(organization_id: @organization.id)
      else
        redirect_to organization_path(@organization)
      end
    else
      flash[:alert] = "Organization could not be created: #{model_error_string(@organization)}"
      render :new
    end
  end

  def show
    @organization = Organization.find id_param

    @tags = Tag.where.not(id: @organization.taggings.pluck(:tag_id))
    @users = user_filter
  end

  def edit
    @organization = Organization.find id_param
  end

  def update
    @organization = Organization.find id_param

    if organization_enabled?(@organization) && npi_blank?
      flash[:alert] = 'Enabled organizations require an NPI.'
      render :edit
    elsif @organization.update organization_params
      flash[:notice] = 'Organization updated.'
      redirect_to organization_path(@organization)
    else
      flash[:alert] = "Organization could not be updated: #{model_error_string(@organization)}"
      render :edit
    end
  end

  def destroy
    @organization = Organization.find id_param
    if @organization.destroy
      flash[:notice] = 'Organization deleted.'
      redirect_to organizations_path
    else
      flash[:alert] = "Organization could not be deleted: #{model_error_string(@organization)}"
      redirect_to organization_path(@organization)
    end
  end

  def add_or_delete
    @organization = Organization.find(params[:organization_id])
    @user = user_identify

    if params[:_method] == 'add'
      add_user_to_org
    elsif params[:_method] == 'delete'
      delete_user_from_org
    else
      redirect_to organization_path(@organization)
    end
  end

  private

  def add_user_to_org
    @user.organizations.clear
    if @organization.users << @user
      flash[:notice] = 'User has been successfully added to the organization.'
      page_redirect
    else
      flash[:alert] = 'User could not be added to the organization.'
    end
  end

  def delete_user_from_org
    if @organization.users.delete(@user)
      flash[:notice] = 'User has been successfully deleted from the organization.'
      page_redirect
    else
      flash[:alert] = 'User could not be deleted from the organization.'
    end
  end

  def npi_blank?
    organization_params[:npi].blank?
  end

  def organization_enabled?(org)
    @reg_org = org.reg_org

    return true if @reg_org.present? && @reg_org.enabled == true
  end

  def org_page_params(results)
    results.page params[:page]
  end

  def page_redirect
    return redirect_to user_url(@user) if params[:user_id].present?

    redirect_to organization_path(@organization)
  end

  def user_id_params
    params.permit(:user_id)
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
