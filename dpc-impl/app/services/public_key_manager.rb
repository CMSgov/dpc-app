# frozen_string_literal: true

class PublicKeyManager
  attr_reader :imp_id, :org_id, :errors

  def initialize(imp_id:, org_id:)
    @imp_id = imp_id
    @org_id = org_id
    @errors = []
  end

  def create_public_key(public_key:, label:, snippet_signature:)
    public_key = strip_carriage_returns(public_key)
    snippet_signature = strip_carriage_returns(snippet_signature)

    if invalid_encoding?(public_key)
      return { response: false,
               message: @errors[0] }
    end

    api_client = ApiClient.new
    api_client.create_public_key(@imp_id, @org_id,
                                 params: { label: label,
                                           public_key: public_key,
                                           snippet_signature: snippet_signature })

    return api_response
  end

  def create_system(org_name:, public_key:, snippet_signature:)
    public_key = strip_carriage_returns(public_key)
    snippet_signature = strip_carriage_returns(snippet_signature)

    if invalid_encoding?(public_key)
      return { response: false,
               message: @errors[0] }
    end

    api_client = ApiClient.new
    api_client.create_system(@imp_id, @org_id,
                             params: { client_name: org_name + " System",
                                       public_key: public_key,
                                       snippet_signature: snippet_signature })

    return api_response(api_client)
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

  def api_response(api_client)
    { response: api_client.response_successful?,
      message: api_client.response_body }
  end

  def strip_carriage_returns(str)
    str.gsub(/\r/, '')
  end
end
