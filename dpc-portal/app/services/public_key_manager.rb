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

    return { response: false, errors: @errors } if invalid_input?(public_key, label, snippet_signature)

    api_client = DpcClient.new
    api_client.create_public_key(api_id,
                                 params: { label:, public_key:, snippet_signature: })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create public key: #{api_client.response_body}"
      parse_errors(api_client.response_body)
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

  def invalid_input?(public_key, label, snippet_signature)
    root_errors = Set.new
    if public_key.present?
      validate_encoding(public_key, root_errors)
    else
      @errors[:public_key] = "Public key can't be blank."
      root_errors << "Fields can't be blank."
    end
    if label && label.length > 25
      @errors[:label] = 'Label must be 25 characters or fewer.'
      root_errors << 'Invalid label.'
    elsif label.blank?
      @errors[:label] = "Label can't be blank."
      root_errors << "Fields can't be blank."
    end
    if snippet_signature.blank?
      @errors[:snippet_signature] = "Snippet signature can't be blank."
      root_errors << "Fields can't be blank."
    end
    @errors[:root] = root_errors.first if root_errors.present?
    handle_root_errors(root_errors)
  end

  def validate_encoding(key_string, root_errors)
    key = OpenSSL::PKey::RSA.new(key_string)
    if key.private?
      @errors[:public_key] = 'Must be a public key (not a private key).'
      root_errors << 'Invalid public key.'
    end
  rescue OpenSSL::PKey::RSAError
    @errors[:public_key] = 'Must be a valid public key.'
    root_errors << 'Invalid public key.'
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

  def strip_carriage_returns(str)
    str&.gsub("\r", '')
  end

  def parse_errors(error_msg)
    if error_msg&.include?('Public key could not be verified')
      @errors[:snippet_signature] = "Signature snippet doesn't match public key."
      @errors[:root] = 'Invalid signature snippet.'
    elsif error_msg&.include?('Public key is not valid')
      @errors[:public_key] = 'Must be a valid public key.'
      @errors[:root] = 'Invalid public key.'
    else
      @errors[:root] = "We're sorry, but we can't complete your request. Please try again tomorrow."
    end
  end
end
