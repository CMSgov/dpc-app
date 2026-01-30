# frozen_string_literal: true

# Simple class for holding OIDC information linked to user
class UserCredential < ApplicationRecord
  belongs_to :user
end
