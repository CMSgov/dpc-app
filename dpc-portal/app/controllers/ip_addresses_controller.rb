# frozen_string_literal: true

# Handles IP address requests
class IpAddressesController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization
  before_action :require_can_access

  def new
    render Page::IpAddress::NewAddressComponent.new(@organization)
  end

  # rubocop:disable Metrics/AbcSize
  def create
    manager = IpAddressManager.new(@organization.dpc_api_organization_id)
    new_ip_address = manager.create_ip_address(ip_address: params[:ip_address], label: params[:label])
    if new_ip_address[:response]
      flash[:notice] = 'IP address successfully created.'
      redirect_to organization_path(@organization)
    else
      flash[:alert] = "IP address could not be created: #{manager.errors.join(', ')}."
      render Page::IpAddress::NewAddressComponent.new(@organization)
    end
  end

  def destroy
    manager = IpAddressManager.new(@organization.dpc_api_organization_id)
    if manager.delete_ip_address(params)
      flash[:notice] = 'IP address successfully deleted.'
    else
      flash[:alert] = "IP address could not be deleted: #{manager.errors.join(', ')}."
    end
    redirect_to organization_path(@organization)
  end
  # rubocop:enable Metrics/AbcSize
end
