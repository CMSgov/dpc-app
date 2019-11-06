# frozen_string_literal: true

class RegisteredOrganization < ApplicationRecord
  belongs_to :organization

  enum api_env: {
    'sandbox' => 0,
    'production' => 1
  }

  validates :api_id, :api_env, :organization, presence: true
end
