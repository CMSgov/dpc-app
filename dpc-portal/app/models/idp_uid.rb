# frozen_string_literal: true

# Simple class for holding OIDC information linked to user
class IdpUid < ApplicationRecord
  belongs_to :user
end
