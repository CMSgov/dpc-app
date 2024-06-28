# frozen_string_literal: true

# Record of an action on a credential type
class CredentialAuditLog < ApplicationRecord
  validates_presence_of %i[credential_type dpc_api_credential_id action]
  enum credential_type: %i[client_token ip_address public_key]
  enum action: %i[add remove]

  # Optional because can be deleted when provider organization disabled
  belongs_to :user, optional: true
end
