# frozen_string_literal: true

# Manages ip addresses for an organization
class IpAddressManager
  require 'ipaddr'
  include CredentialManager

  def create_ip_address(ip_address:)
    sanitized_ip = validate_ip(ip_address)
    return { response: false, errors: @errors } if @errors.present?

    api_client = DpcClient.new
    api_client.create_ip_address(api_id, params: { ip_address: sanitized_ip })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create IP address: #{api_client.response_body}"
      parse_errors(api_client.response_body)
    end

    { response: api_client.response_successful?,
      message: api_client.response_body,
      errors: }
  end

  def delete_ip_address(params)
    sanitized_id = validate_uid(params[:id])
    return false if sanitized_id.nil?

    api_client = DpcClient.new
    api_client.delete_ip_address(api_id, sanitized_id)

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
      entities = api_client.response_body['entities']
      entities.each { |e| e['ip_addr'] = e.dig('ipAddress', 'address') }
      entities
    else
      Rails.logger.warn "Could not get IP addresses: #{api_client.response_body}"
      parse_errors(api_client.response_body) if api_client.response_body.present?
      []
    end
  end

  private

  def validate_ip(addr_string)
    addr_string = addr_string&.gsub("\r", '')&.strip
    
    if addr_string.blank?
      @errors[:ip_address] = "IP address can't be blank."
      return nil
    end

    parsed = IPAddr.new(addr_string)
    handle_root_errors if @root_errors.present?
    parsed.to_s
  rescue IPAddr::InvalidAddressError
    @errors[:ip_address] = 'Invalid IP address.'
    @errors[:root] = 'Invalid IP address.'
    nil
  end

  def validate_uid(id_param)
    return nil if id_param.blank?

    sanitized = id_param.to_s.strip
    sanitized if sanitized.match?(/\A[a-zA-Z0-9\-]{1,64}\z/)
  end

  def parse_errors(error_msg)
    @errors[:root] = if error_msg&.include?('Max Ips for organization reached')
                       'You entered the maximum number of IP addresses.'
                     else
                       SERVER_ERROR_MSG
                     end
  end
end
