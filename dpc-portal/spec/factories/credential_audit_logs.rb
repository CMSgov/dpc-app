# frozen_string_literal: true

FactoryBot.define do
  factory :credential_audit_log do
    user { build(:user) }
    credential_type { :client_token }
    provider_organization { build(:provider_organization) }
    action { :add }
  end
end
