# frozen_string_literal: true

# Manages client tokens for an organization
class ClientTokenManager
  attr_reader :api_id, :client_token

  def initialize(api_id)
    @api_id = api_id
  end

  def create_client_token(label: nil)
    api_client = DpcClient.new
    api_client.create_client_token(api_id, params: { label: label })

    @client_token = api_client.response_body

    api_client.response_successful?
  end

  def delete_client_token(params)
    api_client = DpcClient.new
    api_client.delete_client_token(api_id, params[:id])
    api_client.response_successful?
  end

  def client_tokens
    api_client = DpcClient.new
    api_client.get_client_tokens(api_id)
    if api_client.response_successful?
      api_client.response_body['entities']
    else
      []
    end
  end
end
