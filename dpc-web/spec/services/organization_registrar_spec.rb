# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OrganizationRegistrar do
  describe '#register_all' do
    it 'cleans up old registered org' do
      org = create(:organization, :sandbox_enabled)
      sandbox_reg_org = org.sandbox_registered_organization

      deletion_api_client = instance_double(APIClient)

      allow(APIClient).to receive(:new).with('sandbox').and_return(deletion_api_client)
      allow(deletion_api_client).to receive(:delete_organization).with(sandbox_reg_org).and_return(true)

      expect do
        OrganizationRegistrar.new(organization: org, api_environments: %w[production]).
          register_all
      end.to change(RegisteredOrganization, :count).by(-1)

      expect(deletion_api_client).to have_received(:delete_organization).with(sandbox_reg_org)
    end

    it 'updates existing registered organizations and endpoints' do
      org = create(:organization, :sandbox_enabled)
      sandbox_reg_org = org.sandbox_registered_organization

      api_client = instance_double(APIClient)

      allow(APIClient).to receive(:new).with('sandbox').and_return(api_client)
      allow(api_client).to receive(:update_organization).with(sandbox_reg_org).and_return(true)
      allow(api_client).to receive(:update_endpoint).with(sandbox_reg_org).and_return(true)

      allow(api_client).to receive(:response_successful?).and_return(true)
      allow(api_client).to receive(:response_body).and_return(
        'id' => '8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d',
        'endpoint' => [{ 'reference' => 'Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66' }]
      )

      expect do
        OrganizationRegistrar.new(organization: org, api_environments: %w[sandbox]).
          register_all
      end.to change(RegisteredOrganization, :count).by(0)

      expect(api_client).to have_received(:update_organization).with(sandbox_reg_org)
    end
  end
end
