# frozen_string_literal: true

# Wrapper for calls to dpc-api
class Organization
  attr_reader :api_id, :name, :npi, :found

  def initialize(api_id)
    @api_id = api_id
    @client = DpcClient.new
    data = @client.get_organization(api_id)
    raise DpcRecordNotFound, 'No such organization' unless data

    @found = true
    @name = data.name
    @npi = data.identifier.select { |id| id.system == 'http://hl7.org/fhir/sid/us-npi' }.first&.value
    @keys = @tokens = nil
  end

  def public_keys
    unless @keys
      pkm = PublicKeyManager.new(api_id)
      @keys = pkm.public_keys
    end
    @keys
  end

  def client_tokens
    unless @tokens
      ctm = ClientTokenManager.new(api_id)
      @tokens = ctm.client_tokens
    end
    @tokens
  end

  def public_ips
    [{ 'label' => 'My IP', 'ip_addr' => '91.142.87.5', 'createdAt' => '2021-04-08T12:03:44.823+00:00' }]
  end
end

class DpcRecordNotFound < ActiveRecord::RecordNotFound; end
