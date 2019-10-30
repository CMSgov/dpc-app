# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OrganizationRegistrar do
  describe '#register_all' do
    it 'cleans up old registered org and creates new one' do
      org = create(:organization)
      sandbox_reg_org = create(:registered_organization, api_env: 'sandbox', organization: org)

      deletion_api_client = instance_double(APIClient)
      prod_creation_api_client = instance_double(APIClient)

      allow(APIClient).to receive(:new).with('sandbox').and_return(deletion_api_client)
      allow(deletion_api_client).to receive(:delete_organization).with(sandbox_reg_org).and_return(true)

      allow(APIClient).to receive(:new).with('production').and_return(prod_creation_api_client)
      allow(prod_creation_api_client).to receive(:create_organization).with(org).and_return(
        {
          'id' => '8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d'
        }
      )

      expect do
        OrganizationRegistrar.new(organization: org, api_environments: %w[production]).
          register_all
      end.to change(RegisteredOrganization, :count).by(0)

      expect(deletion_api_client).to have_received(:delete_organization).with(sandbox_reg_org)
      expect(prod_creation_api_client).to have_received(:create_organization).with(org)
    end
  end
end
