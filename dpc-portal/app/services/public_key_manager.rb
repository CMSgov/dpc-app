# frozen_string_literal: true

# Manages public keys for an organization
class PublicKeyManager
  attr_reader :api_id, :errors

  def initialize(api_id)
    @api_id = api_id
    @errors = {}
  end

  def create_public_key(public_key:, label:, snippet_signature:)
    public_key = strip_carriage_returns(public_key)
    snippet_signature = strip_carriage_returns(snippet_signature)

    return { response: false, errors: @errors } unless valid_input?(public_key, label, snippet_signature)

    api_client = DpcClient.new
    api_client.create_public_key(api_id,
                                 params: { label:, public_key:, snippet_signature: })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create public key: #{api_client.response_body}"
      parse_errors(api_client.response_body) if api_client.response_body.present?
    end
    { response: api_client.response_successful?,
      message: api_client.response_body,
      errors: }
  end

  def delete_public_key(params)
    api_client = DpcClient.new
    api_client.delete_public_key(api_id, params[:id])
    Rails.logger.error "Failed to delete public key: #{api_client.response_body}" unless api_client.response_successful?
    api_client.response_successful?
  end

  def public_keys
    api_client = DpcClient.new
    api_client.get_public_keys(api_id)

    if api_client.response_successful?
      api_client.response_body['entities']
    else
      Rails.logger.warn 'Could not get public keys'
      @errors.merge!(api_client.response_body)
      []
    end
  end

  def valid_input?(public_key, label, snippet_signature)
    if public_key.present?
      validate_encoding(public_key)
    else
      @errors[:public_key] = 'Cannot be blank' unless public_key.present?
    end
    if label.present?
      @errors[:label] = 'Label must be 25 characters or fewer' if label.length > 25
    else
      @errors[:label] = 'Cannot be blank'
    end
    @errors[:snippet_signature] = 'Cannot be blank' unless snippet_signature.present?
    @errors.blank?
  end

  def validate_encoding(key_string)
    key = OpenSSL::PKey::RSA.new(key_string)
    @errors[:public_key] = 'Must be a public key' if key.private?
  rescue OpenSSL::PKey::RSAError
    @errors[:public_key] = 'Must have valid encoding'
  end

  def strip_carriage_returns(str)
    str&.gsub("\r", '')
  end

  def parse_errors(error_msg)
    @errors[:snippet_signature] = "Signature doesn't match" if error_msg.include?('Public key could not be verified')
    @errors[:public_key] = 'Must have valid encoding' if error_msg.include?('Public key is not valid')
  end
end
