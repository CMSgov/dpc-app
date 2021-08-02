# frozen_string_literal: true

class ClientTokenManager
  attr_reader :imp_id, :org_id, :errors, :client_token, :created_at

  def initialize(imp_id:, org_id:)
    @imp_id = imp_id
    @org_id = org_id
    @errors = []
  end

  def create_client_token(label: nil)
    api_client = ApiClient.new
    api_client.create_client_token(@imp_id, @org_id, {label: label})

    @client_token = api_client.response_body

    api_client.response_successful?
  end

  def delete_client_token(token_id)
  end
end