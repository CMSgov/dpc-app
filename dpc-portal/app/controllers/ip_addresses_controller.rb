# frozen_string_literal: true

# Handles IP address requests
class IpAddressesController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization

  def new
    render Page::IpAddress::NewAddressComponent.new(@organization)
  end

  # rubocop:disable Metrics/AbcSize
  def create
    manager = IpAddressManager.new(params[:organization_id])
    new_ip_address = manager.create_ip_address(ip_address: params[:ip_address], label: params[:label])
    if new_ip_address[:response]
      flash[:notice] = 'IP address successfully created.'
      redirect_to organization_path(params[:organization_id])
    else
      flash[:alert] = "IP address could not be created: #{manager.errors.join(', ')}."
      render Page::IpAddress::NewAddressComponent.new(@organization)
    end
  end

  def destroy
    manager = IpAddressManager.new(params[:organization_id])
    if manager.delete_ip_address(params)
      flash[:notice] = 'IP address successfully deleted.'
    else
      flash[:alert] = "IP address could not be deleted: #{manager.errors.join(', ')}."
    end
    redirect_to organization_path(params[:organization_id])
  end
  # rubocop:enable Metrics/AbcSize

  private

  def load_organization
    @organization = case ENV.fetch('ENV', nil)
                    when 'prod-sbx'
                      redirect_to root_url
                    when 'test'
                      Organization.new('6a1dbf47-825b-40f3-b81d-4a7ffbbdc270')
                    when 'dev'
                      Organization.new('78d02106-2837-4d07-8c51-8d73332aff09')
                    else
                      Organization.new(params[:organization_id])
                    end
  end
end
