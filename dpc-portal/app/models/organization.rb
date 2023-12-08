# frozen_string_literal: true

# Wrapper for calls to dpc-api
class Organization
  attr_reader :api_id, :name

  def initialize(api_id)
    @api_id = api_id
    @client = DpcClient.new
    data = @client.get_organization(api_id)
    @name = data.name
  end

  def public_keys
    pkm = PublicKeyManager.new(api_id)
    pkm.public_keys
  end

  def client_tokens
    ctm = ClientTokenManager.new(api_id)
    ctm.client_tokens
  end
end
