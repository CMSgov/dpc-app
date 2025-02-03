# frozen_string_literal: true

class RegisteredOrganization < ApplicationRecord
  belongs_to :organization

  before_create :create_api_organization
  after_create :notify_users_of_sandbox_access, if: -> { prod_sbx? }
  before_update :update_api_organization

  validates :organization, presence: true

  def client_tokens
    ClientTokenManager.new(registered_organization: self).client_tokens
  end

  def public_keys
    PublicKeyManager.new(registered_organization: self).public_keys
  end

  def create_api_organization
    api_request = api_service.create_organization(organization)

    api_response = api_request.response_body

    if api_request.response_successful?
      self[:api_id] = api_response['id']
      api_response
    else
      action = 'registered'
      msg = organization.npi.present? ? api_response : 'NPI Required'
      api_error(action, msg)
      throw(:abort)
    end
  end

  def update_api_organization
    api_request = api_service.update_organization(organization, api_id)
    api_response = api_request.response_body
    return if api_request.response_successful?

    action = 'updated (organization)'
    msg = api_response
    api_error(action, msg)
    throw(:abort)
  end

  def notify_users_of_sandbox_access
    organization.notify_users_of_sandbox_access
  end

  def prod_sbx?
    ENV['ENV'] == 'prod-sbx'
  end

  private

  def api_service
    @api_service ||= DpcClient.new
  end

  def api_error(action, msg)
    errors.add(:base, "couldn't be #{action} with API: #{msg}")
  end
end
