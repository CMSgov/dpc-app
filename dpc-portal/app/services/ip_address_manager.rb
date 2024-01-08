# frozen_string_literal: true

# Manages ip addresses for an organization
class IpAddressManager
  require 'ipaddr'

  attr_reader :api_id, :errors

  def initialize(api_id)
    @api_id = api_id
    @errors = []
  end

  def create_ip_address(ip_address:, label:)
    return { response: false, message: @errors[0] } if missing_params(ip_address, label)

    label = strip_carriage_returns(label)
    ip_address = strip_carriage_returns(ip_address)
    return { response: false, message: @errors[0] } if invalid_ip?(ip_address) || label_length?(label)

    api_client = DpcClient.new
    api_client.create_ip_address(api_id, params: { label: label, ip_address: ip_address })

    Rails.logger.error "Failed to create ip address: #{api_client.response_body}" unless api_client.response_successful?
    { response: api_client.response_successful?,
      message: api_client.response_body }
  end

  def delete_ip_address(params)
    api_client = DpcClient.new
    api_client.delete_ip_address(api_id, params[:id])
    Rails.logger.error "Failed to delete ip_address: #{api_client.response_body}" unless api_client.response_successful?
    { response: api_client.response_successful?,
      message: api_client.response_body }
  end

  def ip_addresses
    api_client = DpcClient.new
    api_client.get_ip_addresses(api_id)

    if api_client.response_successful?
      api_client.response_body['entities']
    else
      Rails.logger.warn 'Could not get ip_addresses'
      @errors << api_client.response_body
      []
    end
  end

  private

  def missing_params(ip_address, label)
    if ip_address.blank?
      @errors << 'Missing IP address.'
    elsif label.blank?
      @errors << 'Missing label.'
    end
    ip_address.blank? || label.blank?
  end

  def invalid_ip?(addr_string)
    IPAddr.new(addr_string)
    false
  rescue IPAddr::InvalidAddressError
    @errors << 'Invalid IP address.'
    true
  end

  def label_length?(label)
    if label.length > 25
      @errors << 'Label cannot be over 25 characters.'
      true
    else
      false
    end
  end

  def strip_carriage_returns(str)
    str.gsub("\r", '')
  end
end
