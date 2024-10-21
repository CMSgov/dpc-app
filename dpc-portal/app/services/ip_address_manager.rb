# frozen_string_literal: true

# Manages ip addresses for an organization
class IpAddressManager
  require 'ipaddr'
  include CredentialManager

  def create_ip_address(ip_address:, label:)
    label = strip_carriage_returns(label)
    ip_address = strip_carriage_returns(ip_address)
    return { response: false, errors: @errors } if invalid_input?(ip_address, label)

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

  def invalid_input?(ip_address, label)
    validate_label(label)
    validate_ip_address(ip_address)
    handle_root_errors if @root_errors.present?
    @errors.present?
  end

  def validate_ip_address(addr_string)
    if addr_string.blank?
      @errors[:ip_address] = "IP address can't be blank."
    else
      IPAddr.new(addr_string).blank?
    end
  rescue IPAddr::InvalidAddressError
    @errors[:ip_address] = 'Invalid IP address.'
    @root_errors << 'Invalid IP address.'
  end

  def strip_carriage_returns(str)
    str&.gsub("\r", '')
  end

  def parse_errors(error_msg)
    @errors[:root] = if error_msg&.include?('Max Ips for organization reached')
                       'You entered the maximum number of IP addresses.'
                     else
                       SERVER_ERROR_MSG
                     end
  end
end
