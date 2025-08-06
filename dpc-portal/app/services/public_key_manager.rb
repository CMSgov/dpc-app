# frozen_string_literal: true

# Manages public keys for an organization
class PublicKeyManager
  include CredentialManager

  INVALID_KEY = 'Invalid public key.'

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
    validate_label(label)
    @errors[:public_key] = "Public key can't be blank." if public_key.blank?
    @errors[:snippet_signature] = "Signature snippet can't be blank." if snippet_signature.blank?
    validate_encoding(public_key) if public_key.present?
    handle_root_errors if @root_errors.present?
    @errors.present?
  end

  def validate_encoding(key_string)
    key = OpenSSL::PKey::RSA.new(key_string)
    if key.private?
      @errors[:public_key] = 'Must be a public key (not a private key).'
      @root_errors << INVALID_KEY
    end
  rescue OpenSSL::PKey::RSAError
    @errors[:public_key] = 'Must be a valid public key.'
    @root_errors << INVALID_KEY
  end

  def parse_errors(error_msg)
    if error_msg&.include?('Public key could not be verified')
      @errors[:snippet_signature] = "Signature snippet doesn't match public key."
      @errors[:root] = 'Invalid signature snippet.'
    elsif error_msg&.include?('Public key is not valid')
      @errors[:public_key] = 'Must be a valid public key.'
      @errors[:root] = INVALID_KEY
    elsif error_msg&.include?('duplicate key value violates unique constraint')
      @errors[:public_key] = I18n.t('errors.duplicate_key.text')
      @errors[:root] = INVALID_KEY
    else
      @errors[:root] = SERVER_ERROR_MSG
    end
  end
end
