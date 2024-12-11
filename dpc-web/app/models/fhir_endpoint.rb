# frozen_string_literal: true

class FhirEndpoint < ApplicationRecord
  belongs_to :registered_organization

  enum :status, {
    'test' => 0,
    'active' => 1,
    'suspended' => 2,
    'error' => 3,
    'off' => 4,
    'entered-in-error' => 5
  }

  validates :name, :uri, :status, presence: true
  validate :uri_is_valid_format

  def uri_is_valid_format
    parsed_uri = URI.parse(self[:uri])
    errors.add :uri, 'must be valid URI present' if parsed_uri.host.nil?
  rescue URI::InvalidURIError
    errors.add :uri, 'must be valid URI (must have valid domain name)'
  end
end
