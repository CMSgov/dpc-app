# frozen_string_literal: true

class PublicKeyManager
  attr_reader :registered_organization, :errors

  def initialize(registered_organization:)
    @registered_organization = registered_organization
    @errors = []
  end

  def create_public_key(public_key:, label:,
                        snippet_signature:)
    public_key = strip_carriage_returns(public_key)
    snippet_signature = strip_carriage_returns(snippet_signature)

    if invalid_encoding?(public_key)
      return { response: false,
               message: @errors[0] }
    end

    api_client = APIClient.new
    api_client.create_public_key(registered_organization.api_id,
                                 params: { label: label, public_key: public_key,
                                           snippet_signature: snippet_signature })

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

  def public_keys
    api_client = APIClient.new
    api_client.get_public_keys(registered_organization.api_id)

    if api_client.response_successful?
      api_client.response_body['entities']
    else
      Rails.logger.warn 'Could not get public keys'
      @errors << api_client.response_body
      []
    end
  end

  def strip_carriage_returns(str)
    str.gsub(/\r/, '')
  end
end
