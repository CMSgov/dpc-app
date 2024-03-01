# frozen_string_literal: true

# Handles IP address requests
class IpAddressesController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization

  def new
    render Page::IpAddress::NewAddressComponent.new(@organization)
  end

  def create
    manager = IpAddressManager.new(@organization.dpc_api_organization_id)
    new_ip_address = manager.create_ip_address(ip_address: params[:ip_address], label: params[:label])
    if new_ip_address[:response]
      create_success
    else
      create_failure(manager.errors.join(', '))
    end
  end

  def destroy
    manager = IpAddressManager.new(@organization.dpc_api_organization_id)
    if manager.delete_ip_address(params)
      flash[:notice] = 'IP address successfully deleted.'
    else
      flash[:alert] = "IP address could not be deleted: #{manager.errors.join(', ')}."
    end
    redirect_to organization_path(params[:organization_id])
  end

  private

  def create_success
    flash[:notice] = 'IP address successfully created.'
    redirect_to organization_path(params[:organization_id])
  end

  def create_failure(errors)
    flash[:alert] = "IP address could not be created: #{errors}."
    render Page::IpAddress::NewAddressComponent.new(@organization)
  end
end
