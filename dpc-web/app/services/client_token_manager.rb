# frozen_string_literal: true

class ClientTokenManager
  attr_reader :api_env, :organization, :client_token

  def initialize(api_env:, organization:)
    @api_env = api_env
    @organization = organization
  end

  def create_client_token(label: nil)
    api_client = APIClient.new(api_env)
    api_client.create_client_token(registered_org.api_id, params: { label: label })

    @client_token = api_client.response_body

    api_client.response_successful?
  end

  def client_tokens
    api_client = APIClient.new(api_env)
    api_client.get_client_tokens(registered_org.api_id)
    api_client.response_body['entities']
  end

  # If no registered_org, attempt creation and then proceed
  def registered_org
    @registered_org ||= organization.registered_organizations.find_by(api_env: api_env)
  end
end
