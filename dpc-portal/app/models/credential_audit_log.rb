# frozen_string_literal: true

# Record of an action on a credential type
class CredentialAuditLog < ApplicationRecord
  validates_presence_of %i[credential_type action]
  enum credential_type: %i[client_token ip_address public_key]
  enum action: %i[add remove]

  belongs_to :user, optional: true
  belongs_to :provider_organization
end
