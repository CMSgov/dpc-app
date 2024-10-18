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
    root_errors = Set.new
    if label && label.length > 25
      @errors[:label] = 'Label must be 25 characters or fewer.'
      root_errors << 'Invalid label.'
    elsif label.blank?
      @errors[:label] = "Label can't be blank."
      root_errors << "Fields can't be blank."
    end
    if ip_address.present?
      unless valid_ip_address?(ip_address)
        @errors[:ip_address] = 'Invalid IP address.'
        root_errors << 'Invalid IP address.'
      end
    else
      @errors[:ip_address] = "IP address can't be blank."
      root_errors << "Fields can't be blank."
    end
    handle_root_errors(root_errors)
  end

  def handle_root_errors(root_errors)
    case root_errors.size
    when 0
      false
    when 1
      @errors[:root] = root_errors.first
    else
      @errors[:root] = %(Errors:<ul>#{root_errors.map { |e| "<li>#{e}</li>" }.join}</ul>)
    end
  end

  def valid_ip_address?(addr_string)
    IPAddr.new(addr_string)
  rescue IPAddr::InvalidAddressError
    false
  end

  def strip_carriage_returns(str)
    str&.gsub("\r", '')
  end

  def parse_errors(error_msg)
    @errors[:root] = if error_msg&.include?('Max Ips for organization reached')
                       'You entered the maximum number if IP addresses.'
                     else
                       "We're sorry, but we can't complete your request. Please try again tomorrow."
                     end
  end
end
