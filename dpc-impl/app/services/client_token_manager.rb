# frozen_string_literal: true

class ClientTokenManager
  attr_reader :imp_id, :org_id, :errors

  def initialize(imp_id:, org_id:)
    @imp_id = imp_id
    @org_id = org_id
    @errors = []
  end

  def create_client_token(label: nil)
  end

  def delete_client_token(token_id)
  end
end