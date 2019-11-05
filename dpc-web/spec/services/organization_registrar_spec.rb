# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OrganizationRegistrar do
  describe '#register_all' do
    it 'cleans up old registered org and creates new one' do
      org = create(:organization, :with_endpoint, api_environments: [0])
      sandbox_reg_org = create(:registered_organization, api_env: 'sandbox', organization: org)

      deletion_api_client = instance_double(APIClient)
      prod_creation_api_client = instance_double(APIClient)

      allow(APIClient).to receive(:new).with('sandbox').and_return(deletion_api_client)
      allow(deletion_api_client).to receive(:delete_organization).with(sandbox_reg_org).and_return(true)

      allow(APIClient).to receive(:new).with('production').and_return(prod_creation_api_client)
      allow(prod_creation_api_client).to receive(:create_organization).with(org).
        and_return(prod_creation_api_client)
      allow(prod_creation_api_client).to receive(:response_successful?).and_return(true)
      allow(prod_creation_api_client).to receive(:response_body).and_return(
        {
          'id' => '8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d',
          'endpoint' => [{ 'reference' => 'Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66' }]
        }
      )

      initial_profile_endpoint_count = ProfileEndpoint.count

      expect do
        OrganizationRegistrar.new(organization: org, api_environments: %w[production]).
          register_all
      end.to change(RegisteredOrganization, :count).by(0)

      expect(ProfileEndpoint.count).to eq(initial_profile_endpoint_count)
      expect(deletion_api_client).to have_received(:delete_organization).with(sandbox_reg_org)
      expect(prod_creation_api_client).to have_received(:create_organization).with(org)
    end

    it 'creates a test profile_endpoint for a sandbox org without one' do
      org = create(:organization)
      api_client = instance_double(APIClient)

      allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
      allow(api_client).to receive(:create_organization).with(org).and_return(api_client)
      allow(api_client).to receive(:response_successful?).and_return(true)
      allow(api_client).to receive(:response_body).and_return(
        {
          'id' => '8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d',
          'endpoint' => [{ 'reference' => 'Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66' }]
        }
      )

      expect do
        OrganizationRegistrar.new(organization: org, api_environments: %w[sandbox]).
          register_all
      end.to change(ProfileEndpoint, :count).by(1)

      expect(ProfileEndpoint.last.organization).to eq(org)
    end
  end
end
