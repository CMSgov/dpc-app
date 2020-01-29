require 'rails_helper'

RSpec.describe RegisteredOrganization, type: :model do
  describe '#client_tokens' do
    it 'gets array from ClientTokenManager' do
      org = create(:organization)
      registered_org = create(:registered_organization, organization: org, api_env: 0)
      tokens = [{'token' => 'abcdef'}, {'token' => 'ftguiol'}]

      manager = instance_double(ClientTokenManager)
      allow(ClientTokenManager).to receive(:new).with(api_env: 'sandbox', organization: org)
                                                .and_return(manager)
      allow(manager).to receive(:client_tokens).and_return(tokens)

      expect(registered_org.client_tokens).to eq(tokens)
    end
  end

  describe '#public_keys' do
    it 'gets array from PublicKeyManager' do
      org = create(:organization)
      registered_org = create(:registered_organization, organization: org, api_env: 0)
      keys = [{'id' => 'abcdef'}, {'id' => 'ftguiol'}]

      manager = instance_double(PublicKeyManager)
      allow(PublicKeyManager).to receive(:new).with(api_env: 'sandbox', organization: org)
                                                .and_return(manager)
      allow(manager).to receive(:public_keys).and_return(keys)

      expect(registered_org.public_keys).to eq(keys)
    end
  end

  describe '#create_api_organization' do
    let(:registered_organization) { create(:registered_organization, api_id: nil, api_endpoint_ref: nil) }
    let(:mocked_api_response_body) {
      {
        'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad',
        'endpoint' => [
          'reference' => 'Endpoint/437f7b17-3d48-4654-949d-57ea80f8f1d7'
        ]
      }
    }
    let(:api_client) { instance_double(APIClient) }

    before(:each) do
      allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
      allow(api_client).to receive(:create_organization)
        .with(registered_organization.organization, fhir_endpoint: registered_organization.fhir_endpoint.attributes)
        .and_return(api_client)
    end

    it 'invokes APIClient and returns the response body' do
      allow(registered_organization.organization).to receive(:notify_users_of_sandbox_access)
      allow(api_client).to receive(:response_successful?).and_return(true)
      allow(api_client).to receive(:response_body).and_return(mocked_api_response_body)

      expect(registered_organization.create_api_organization).to eq(mocked_api_response_body)
      expect(api_client).to have_received(:create_organization).once
    end

    context 'successful API response' do
      it 'updates attributes and notifies users' do
        allow(registered_organization.organization).to receive(:notify_users_of_sandbox_access)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return(mocked_api_response_body)

        expect(registered_organization.create_api_organization).to eq(mocked_api_response_body)
        expect(registered_organization.organization).to have_received(:notify_users_of_sandbox_access).once
      end
    end

    context 'unsuccessful API response'do
      it 'does not attributes or notify users' do
        allow(registered_organization.organization).to receive(:notify_users_of_sandbox_access)
        allow(api_client).to receive(:response_successful?).and_return(false)
        allow(api_client).to receive(:response_body).and_return({'issues' => ['Bad Request']})

        expect(registered_organization.create_api_organization).to eq({'issues' => ['Bad Request']})
        expect(registered_organization.api_id).to be_nil
        expect(registered_organization.api_endpoint_ref).to be_nil
        expect(registered_organization.organization).not_to have_received(:notify_users_of_sandbox_access)
      end
    end
  end

  describe '#update_api_organization' do
    xit 'makes update API request'
  end

  describe '#build_default_fhir_endpoint' do
    it 'builds fhir_endpoint with default attributes' do
      reg_org = build(:registered_organization)

      reg_org.build_default_fhir_endpoint
      fhir_endpoint = reg_org.fhir_endpoint

      expect([fhir_endpoint.name, fhir_endpoint.status, fhir_endpoint.uri])
        .to eq(['DPC Sandbox Test Endpoint', 'test', 'https://dpc.cms.gov/test-endpoint'])
    end
  end
end
