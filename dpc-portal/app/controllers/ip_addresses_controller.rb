# frozen_string_literal: true

# Handles IP address requests
class IpAddressesController < ApplicationController
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
      manager.errors << new_ip_address[:message]
      render_error('IP address could not be created.')
    end
  end

  def destroy
    manager = IpAddressManager.new(params[:organization_id])
    response = manager.delete_ip_address(params)
    if response[:response]
      flash[:notice] = 'IP address successfully deleted.'
    else
      manager.errors << response[:message]
      flash[:alert] = 'IP address could not be deleted.'
    end
    redirect_to organization_path(params[:organization_id])
  end
  # rubocop:enable Metrics/AbcSize

  private

  def render_error(msg)
    flash[:alert] = msg
    render Page::IpAddress::NewAddressComponent.new(@organization)
  end

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
