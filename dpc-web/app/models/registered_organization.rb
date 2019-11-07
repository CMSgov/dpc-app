# frozen_string_literal: true

class RegisteredOrganization < ApplicationRecord
  belongs_to :organization

  enum api_env: {
    'sandbox' => 0,
    'production' => 1
  }

  validates :api_id, :api_env, :organization, presence: true

  def client_tokens
    ClientTokenManager.new(api_env: api_env, organization: organization).client_tokens
  end

  def public_keys
    # [{'id' => '4r85cfb4-dc36-4cd0-b8f8-400a6dea2d66', 'label' => 'Test Token 1', 'createdAt' => Time.now.iso8601, 'expiresAt' => Time.now.iso8601}]
    PublicKeyManager.new(api_env: api_env, organization: organization).public_keys
  end
end
