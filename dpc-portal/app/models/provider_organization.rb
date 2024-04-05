# frozen_string_literal: true

# Link class to dpc-api Organization
class ProviderOrganization < ApplicationRecord
  validates :npi, presence: true

  belongs_to :terms_of_service_accepted_by, class_name: 'User', required: false

  has_many :ao_org_links
  has_many :cd_org_links

  after_create do
    SyncOrganizationJob.perform_later(id) unless dpc_api_organization_id.present?
  end

  def public_keys
    @keys ||= []
    if dpc_api_organization_id.present?
      pkm = PublicKeyManager.new(dpc_api_organization_id)
      @keys = pkm.public_keys
    end
    @keys
  end

  def client_tokens
    @tokens ||= []
    if dpc_api_organization_id.present?
      ctm = ClientTokenManager.new(dpc_api_organization_id)
      @tokens = ctm.client_tokens
    end
    @tokens
  end

  def public_ips
    @ips ||= []
    if dpc_api_organization_id.present?
      ipm = IpAddressManager.new(dpc_api_organization_id)
      @ips = ipm.ip_addresses
    end
    @ips
  end

  def api_id
    id
  end

  def path_id
    id
  end
end
