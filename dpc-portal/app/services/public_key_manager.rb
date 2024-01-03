# frozen_string_literal: true

# Manages public keys for an organization
class PublicKeyManager
  attr_reader :api_id, :errors

  def initialize(api_id)
    @api_id = api_id
    @errors = []
  end

  def create_public_key(public_key:, label:, snippet_signature:)
    public_key = strip_carriage_returns(public_key)
    snippet_signature = strip_carriage_returns(snippet_signature)

    return { response: false, message: @errors[0] } if invalid_encoding?(public_key)

    api_client = DpcClient.new
    api_client.create_public_key(api_id,
                                 params: { label: label, public_key: public_key,
                                           snippet_signature: snippet_signature })

    Rails.logger.error "Failed to create public key: #{api_client.response_body}" unless api_client.response_successful?
    { response: api_client.response_successful?,
      message: api_client.response_body }
  end

  def invalid_encoding?(key_string)
    key = OpenSSL::PKey::RSA.new(key_string)
    if key.private?
      @errors << 'Must be a public key'
      true
    else
      false
    end
  rescue OpenSSL::PKey::RSAError
    @errors << 'Must have valid encoding'
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
      @errors << api_client.response_body
      []
    end
  end

  def strip_carriage_returns(str)
    str.gsub("\r", '')
  end
end
