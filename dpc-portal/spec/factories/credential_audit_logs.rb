# frozen_string_literal: true

FactoryBot.define do
  factory :credential_audit_log do
    user { build(:user) }
    credential_type { :client_token }
    dpc_api_credential_id { SecureRandom.uuid }
    action { :add }
  end
end
