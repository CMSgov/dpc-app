# frozen_string_literal: true

class PublicKeyManager
  attr_reader :api_env, :organization, :errors

  def initialize(api_env:, organization:)
    @api_env = api_env
    @organization = organization
    @errors = []
  end

  def create_public_key(public_key:, label:)
    public_key = strip_carriage_returns(public_key)
    return false if invalid_encoding?(public_key)

    api_client = APIClient.new(api_env)
    api_client.create_public_key(registered_org.api_id, params: { label: label, public_key: public_key })

    api_client.response_successful?
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
    api_client = APIClient.new(api_env)
    api_client.get_public_keys(registered_org.api_id)

    if api_client.response_successful?
      api_client.response_body['entities']
    else
      Rails.logger.warn 'Could not get public keys'
      @errors << api_client.response_body
      []
    end
  end

  def registered_org
    @registered_org ||= organization.registered_organizations.find_by(api_env: api_env)
  end

  def strip_carriage_returns(str)
    str.gsub(/\r/, '')
  end
end
