# frozen_string_literal: true

# Manages public keys for an organization
class PublicKeyManager
  attr_reader :api_id, :errors

  SIGNATURE_FAIULRE_MESSAGES = ['Key and signature do not match',
                                'Signature could not be verified.',
                                'Public key could not be verified'].freeze

  def initialize(api_id)
    @api_id = api_id
    @errors = {}
  end

  def create_public_key(public_key:, label:, snippet_signature:)
    public_key = strip_carriage_returns(public_key)
    snippet_signature = strip_carriage_returns(snippet_signature)
    return { response: false, errors: @errors } if invalid_input?(public_key, label, snippet_signature)

    return { response: false, errors: @errors } if invalid_encoding?(public_key)

    api_client = DpcClient.new
    api_client.create_public_key(api_id,
                                 params: { label:, public_key:,
                                           snippet_signature: })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create public key: #{api_client.response_body}"
      if api_client.response_body&.include?('Public key could not be verified')
        @errors[:snippet_signature] =
          "Signature doesn't match"
      end
    end
    { response: api_client.response_successful?,
      message: api_client.response_body,
      errors: }
  end

  def invalid_encoding?(key_string)
    key = OpenSSL::PKey::RSA.new(key_string)
    if key.private?
      @errors[:public_key] = 'Must be a public key'
      true
    else
      false
    end
  rescue OpenSSL::PKey::RSAError
    @errors[:public_key] = 'Must have valid encoding'
    true
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
    @errors[:public_key] = 'Cannot be blank' unless public_key.present?
    @errors[:label] = 'Cannot be blank' unless label.present?
    @errors[:snippet_signature] = 'Cannot be blank' unless snippet_signature.present?
    @errors[:label] = 'Label must be 25 characters or fewer' if label.length > 25
    @errors.present?
  end

  def strip_carriage_returns(str)
    str.gsub("\r", '')
  end
end
