# frozen_string_literal: true

# Manages ip addresses for an organization
class IpAddressManager
  require 'ipaddr'

  attr_reader :api_id, :errors

  def initialize(api_id)
    @api_id = api_id
    @errors = []
  end

  # rubocop:disable Metrics/AbcSize
  def create_ip_address(ip_address:, label:)
    if missing_params(ip_address, label)
      return { response: false, message: "Failed to create IP address: #{@errors.join(', ')}." }
    end

    label = strip_carriage_returns(label)
    ip_address = strip_carriage_returns(ip_address)
    if invalid_ip?(ip_address) || label_length?(label)
      return { response: false, message: "Failed to create IP address: #{@errors.join(', ')}." }
    end

    api_client = DpcClient.new
    api_client.create_ip_address(api_id, params: { label:, ip_address: })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create IP address: #{api_client.response_body}"
      @errors << (api_client.response_body || 'failed to create IP address')
    end

    { response: api_client.response_successful?,
      message: api_client.response_body }
  end
  # rubocop:enable Metrics/AbcSize

  def delete_ip_address(params)
    api_client = DpcClient.new
    api_client.delete_ip_address(api_id, params[:id])

    unless api_client.response_successful?
      Rails.logger.error "Failed to delete IP address: #{api_client.response_body}"
      @errors << (api_client.response_body || 'failed to delete IP address')
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
      @errors << api_client.response_body
      []
    end
  end

  private

  def missing_params(ip_address, label)
    @errors << 'missing label' if label.blank?
    @errors << 'missing IP address' if ip_address.blank?
    ip_address.blank? || label.blank?
  end

  def invalid_ip?(addr_string)
    IPAddr.new(addr_string)
    false
  rescue IPAddr::InvalidAddressError
    @errors << 'invalid IP address'
    true
  end

  def label_length?(label)
    @errors << 'label cannot be over 25 characters' if label.length > 25
    label.length > 25
  end

  def strip_carriage_returns(str)
    str.gsub("\r", '')
  end
end
