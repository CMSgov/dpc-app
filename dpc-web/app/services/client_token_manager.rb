# frozen_string_literal: true

class ClientTokenManager
  attr_reader :api_env, :organization, :client_token

  def initialize(api_env:, organization:)
    @api_env = api_env
    @organization = organization
  end

  def create_client_token(label: nil)
    api_client = APIClient.new(api_env).create_client_token(registered_org, params: { label: label })

    @client_token = api_client.response_body['token']
  end

  # If not registered_org, attempt creation and then proceed
  def registered_org
    @registered_org ||= organization.registered_org.find_by(api_env: api_env)
  end
end
