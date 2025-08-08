# frozen_string_literal: true

# A service that onboards an organization outside the portal
class OnboardService
  attr_accessor(:organization_id, :client_token)
  attr_reader(:name, :npi, :public_key, :snippet_signature, :public_key_id, :registered_organization)

  def initialize(name, npi, public_key, snippet_signature)
    raise ArgumentError, 'no blanks' if [name, npi, public_key, snippet_signature].any?(&:blank?)

    @name = name
    @npi = npi
    @public_key = format_key(public_key)
    @snippet_signature = snippet_signature.gsub(' ', '')
  end

  # rubocop:disable Metrics/AbcSize
  def create_organization
    api_response = api_client.get_organization_by_npi(npi)
    if api_response.entry.empty?
      org = OrgObject.new(name, npi)
      create_org_response = api_client.create_organization(org)
      raise OnboardServiceError, create_org_response.response_body.to_s unless create_org_response.response_successful?

      @organization_id = create_org_response.response_body['id']

    elsif api_response.entry.length == 1
      @organization_id = api_response.entry.first.resource.id
    else
      raise OnboardServiceError, "multiple orgs found for NPI #{npi} in dpc_attribution"
    end
    @registered_organization = RegisteredOrganization.new(api_id: @organization_id)
  end
  # rubocop:enable Metrics/AbcSize

  def upload_key
    manager = PublicKeyManager.new(registered_organization:)
    response = manager.create_public_key(label:, public_key:, snippet_signature:)
    raise OnboardServiceError, response[:errors]&.to_s unless response[:response]

    @public_key_id = response.dig(:message, 'id')
  end

  def retrieve_client_token
    manager = ClientTokenManager.new(registered_organization:)
    response = manager.create_client_token(label:)
    raise OnboardServiceError, response[:errors]&.to_s unless response[:response]

    @client_token = response.dig(:message, 'token')
  end

  def encrypted(thing)
    @public_key_encryptor ||= OpenSSL::PKey::RSA.new(public_key)
    Base64.encode64(@public_key_encryptor.public_encrypt(thing))
  end

  private

  # the expectation is that the public key will be added in gha, which replaces \n with ' '
  # this method undoes that
  def format_key(public_key)
    if public_key.include?('- ')
      bits = /(-+[^-]+-+)([^-]+)(-+[^-]+-+)/.match(public_key)
      return "#{bits[1]}#{bits[2].gsub(' ', "\n")}#{bits[3]}" if bits
    end
    public_key
  end

  def api_client
    @api_client ||= DpcClient.new
  end

  def label
    'Onboarding'
  end
end

class OnboardServiceError < StandardError; end

# Org object required to pass to DpcClient#create_organization
class OrgObject
  # rubocop:disable Naming/VariableNumber
  attr_reader :npi, :name, :address_use, :address_type, :address_city, :address_state, :address_street,
              :address_street_2, :address_zip

  def initialize(name, npi)
    randy = Random.new
    @name = name
    @npi = npi
    @address_use = 'work'
    @address_type = 'both'
    @address_street = "#{randy.rand(1000)} Elm Street"
    @address_street_2 = "Suite #{randy.rand(100)}"
    @address_city = 'Akron'
    @address_state = 'OH'
    @address_zip = '22222'
  end
  # rubocop:enable Naming/VariableNumber
end
