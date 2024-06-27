# frozen_string_literal: true

FactoryBot.define do
  factory :credential_audit_log do
    user { build(:user) }
    dpc_api_credential_id { SecureRandom.uuid }
    credential_type { :client_token }
    action { :add }
  end
end
