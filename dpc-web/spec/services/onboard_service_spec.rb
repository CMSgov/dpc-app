# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe OnboardService do
  include DpcClientSupport

  let(:name) { 'Health Hut' }
  let(:npi) { '3624913885' }
  let(:public_key) { file_fixture('stubbed_key.pem').read }
  let(:snippet_signature) { 'stubbed_sign_txt_signature' }

  describe 'new' do
    it 'initializes the attributes' do
      service = OnboardService.new(name,
                                   npi,
                                   public_key,
                                   snippet_signature)
      expect(service.name).to eq name
      expect(service.npi).to eq npi
      expect(service.public_key).to eq public_key
      expect(service.snippet_signature).to eq snippet_signature
    end
    it 'formats public key' do
      pk = public_key.gsub("\n", ' ')
      service = OnboardService.new(name,
                                   npi,
                                   pk,
                                   snippet_signature)
      expect(service.public_key).to eq public_key
    end
    it 'formats signature' do
      spacey_sig = ' signature with spaces '
      service = OnboardService.new(name,
                                   npi,
                                   public_key,
                                   spacey_sig)
      expect(service.snippet_signature).to eq 'signaturewithspaces'
    end
    it 'verifies no blanks' do
      [
        [' ', npi, public_key, snippet_signature],
        [name, ' ', public_key, snippet_signature],
        [name, npi, ' ', snippet_signature],
        [name, npi, public_key, ' ']
      ].each do |args|
        expect { OnboardService.new(*args) }.to raise_error(ArgumentError)
      end
    end
  end

  describe 'create organization' do
    let(:service) do
      OnboardService.new(name, npi, public_key, snippet_signature)
    end

    let(:mock_dpc_client) { instance_double(DpcClient) }
    let(:create_organization_success_response) do
      {
        'resourceType' => 'Organization',
        'id' => '352dda55-6925-4bdb-bcee-1af0bc163699'
      }
    end
    let(:create_organization_unsuccessful_response) do
      MockOrgResponse.new(response_successful: false, response_body: {})
    end
    let(:get_organization_zero_entries) { MockFHIRResponse.new(entries_count: 0) }
    let(:get_organization_one_entry) { MockFHIRResponse.new(entries_count: 1) }
    let(:get_organization_two_entries) { MockFHIRResponse.new(entries_count: 2) }
    it 'creates an org if api_client returns no existing entry' do
      api_client = stub_api_call(message: :get_organization_by_npi,
                                 response: get_organization_zero_entries)

      stub_api_client(message: :create_organization,
                      api_client:,
                      response: create_organization_success_response)

      service.create_organization
      expect(service.organization_id).to eq '352dda55-6925-4bdb-bcee-1af0bc163699'
    end

    it 'sets organization_id if api_client returns an entry' do
      stub_api_call(message: :get_organization_by_npi,
                    response: get_organization_one_entry)
      service.create_organization
      expect(service.organization_id).to eq get_organization_one_entry.entry[0].resource.id
    end

    it 'raises an error if multiple orgs are found for the NPI in dpc_attribution' do
      stub_api_call(message: :get_organization_by_npi,
                      response: get_organization_two_entries)
      expect do
        service.create_organization
      end.to raise_error(OnboardServiceError,
                         "multiple orgs found for NPI #{npi} in dpc_attribution")
    end

    it 'raises an error if api_client.create_organization is unsuccessful' do
      api_client = stub_api_call(message: :get_organization_by_npi,
                                   response: get_organization_zero_entries)

      response = { msg: 'No Worky' }
      stub_api_client(message: :create_organization,
                      api_client:,
                      response:,
                      success: false)
      expect do
        service.create_organization
      end.to raise_error(OnboardServiceError, response.to_s)
    end
  end

  describe 'upload key' do
    let(:organization_id) { 'whatever' }
    let(:service) do
      OnboardService.new(name, npi, public_key, snippet_signature)
    end

    before do
      service.organization_id = organization_id
    end
    it 'handles success' do
      manager = instance_double(PublicKeyManager)
      expect(PublicKeyManager).to receive(:new).and_return(manager)
      expect(manager).to receive(:create_public_key)
        .with({ public_key:,
                snippet_signature:,
                label: 'Onboarding' })
        .and_return({
                      response: true,
                      message: { 'id' => 'some-id' },
                      errors: {}
                    })
      service.upload_key
      expect(service.public_key_id).to eq 'some-id'
    end
  end

  describe 'retrieve token' do
    let(:organization_id) { 'whatever' }
    let(:service) do
      OnboardService.new(name, npi, public_key, snippet_signature)
    end

    before do
      service.organization_id = organization_id
    end
    it 'handles success' do
      manager = instance_double(ClientTokenManager)
      expect(ClientTokenManager).to receive(:new).and_return(manager)
      expect(manager).to receive(:create_client_token)
        .with({ label: 'Onboarding' })
        .and_return({
                      response: true,
                      message: { 'token' => 'the client token' },
                      errors: {}
                    })
      service.retrieve_client_token
      expect(service.client_token).to eq 'the client token'
    end
  end
end
