# frozen_string_literal: true

# Manages ip addresses for an organization
class IpAddressManager
  require 'ipaddr'

  attr_reader :api_id, :errors

  def initialize(api_id)
    @api_id = api_id
    @errors = {}
  end

  def create_ip_address(ip_address:, label:)
    label = strip_carriage_returns(label)
    ip_address = strip_carriage_returns(ip_address)
    return { response: false, errors: @errors } unless valid_input?(ip_address, label)

    api_client = DpcClient.new
    api_client.create_ip_address(api_id, params: { label:, ip_address: })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create IP address: #{api_client.response_body}"
      parse_errors(api_client.response_body)
    end

    { response: api_client.response_successful?,
      message: api_client.response_body,
      errors: }
  end

  def delete_ip_address(params)
    api_client = DpcClient.new
    api_client.delete_ip_address(api_id, params[:id])

    unless api_client.response_successful?
      Rails.logger.error "Failed to delete IP address: #{api_client.response_body}"
      parse_errors(api_client.response_body) if api_client.response_body.present?
    end

    api_client.response_successful?
  end

  def ip_addresses
    api_client = DpcClient.new
    api_client.get_ip_addresses(api_id)

    if api_client.response_successful?
      api_client.response_body['entities']
    else
      Rails.logger.warn "Could not get IP addresses: #{api_client.response_body}"
      parse_errors(api_client.response_body) if api_client.response_body.present?
      []
    end
  end

  private

  def valid_input?(ip_address, label)
    if label.present?
      @errors[:label] = 'Label must be 25 characters or fewer' if label.length > 25
    else
      @errors[:label] = 'Cannot be blank'
    end
    if ip_address.present?
      validate_ip_address(ip_address)
    else
      @errors[:ip_address] = 'Cannot be blank'
    end
    @errors.blank?
  end

  def validate_ip_address(addr_string)
    IPAddr.new(addr_string)
  rescue IPAddr::InvalidAddressError
    @errors[:ip_address] = 'invalid IP address'
  end

  def strip_carriage_returns(str)
    str&.gsub("\r", '')
  end

  def parse_errors(error_msg)
    max_msg = 'Max Ips for organization reached'
    @errors[:root] = if error_msg&.include?(max_msg)
                       max_msg
                     else
                       'Unable to process request'
                     end
  end
end
