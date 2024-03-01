# frozen_string_literal: true

# Link class to dpc-api Organization
class ProviderOrganization < ApplicationRecord
  attr_accessor :keys, :tokens, :ips

  validates :npi, presence: true

  alias_attribute :api_id, :dpc_api_organization_id

  def api_org
    @client = DpcClient.new
    data = @client.get_organization(dpc_api_organization_id)
    raise DpcRecordNotFound, 'No such organization' unless data

    data
  end

  def public_keys
    unless @keys
      pkm = PublicKeyManager.new(dpc_api_organization_id)
      @keys = pkm.public_keys
    end
    @keys
  end

  def client_tokens
    unless @tokens
      ctm = ClientTokenManager.new(dpc_api_organization_id)
      @tokens = ctm.client_tokens
    end
    @tokens
  end

  def ip_addresses
    unless @ips
      ipm = IpAddressManager.new(dpc_api_organization_id)
      @ips = ipm.ip_addresses
    end
    @ips
  end
end

class DpcRecordNotFound < ActiveRecord::RecordNotFound; end
