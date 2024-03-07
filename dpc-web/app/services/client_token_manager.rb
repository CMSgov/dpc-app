# frozen_string_literal: true

class ClientTokenManager
  attr_reader :registered_organization, :client_token

  def initialize(registered_organization:)
    @registered_organization = registered_organization
  end

  def create_client_token(label: nil)
    api_client = DpcClient.new
    api_client.create_client_token(registered_organization.api_id, params: { label: })

    @client_token = api_client.response_body

    api_client.response_successful?
  end

  def delete_client_token(params)
    api_client = DpcClient.new
    api_client.delete_client_token(registered_organization.api_id, params[:id])
  end

  def client_tokens
    api_client = DpcClient.new
    api_client.get_client_tokens(registered_organization.api_id)
    if api_client.response_successful?
      api_client.response_body['entities']
    else
      []
    end
  end
end
