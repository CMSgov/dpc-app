# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  describe 'api_environments=' do
    it 'rejects non-arrays and sets attribute to []' do
      org = build(:organization, api_environments: 'not_array')
      expect(org.api_environments).to eq([])
    end

    it 'rejects blank items in array' do
      org = build(:organization, api_environments: ['', nil, 1])
      expect(org.api_environments).to eq([1])
    end
  end

  describe 'callbacks' do
    describe 'update_api_organization' do
      it 'sends delete command if env is removed on upate' do
        org = build(:organization, api_environments: [0])

        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).with(0).and_return(api_client)
        allow(api_client).to receive(:create_organization).with(org)
        allow(api_client).to receive(:delete_organization).with(org)

        # finish creation with [0]
        org.save

        # remove api_env
        org.update(api_environments: [])

        expect(api_client).to have_received(:delete_organization).with(org)
      end

      it 'sends create command if env is added on update' do
        org = create(:organization, api_environments: [])

        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).with(0).and_return(api_client)
        allow(api_client).to receive(:create_organization).with(org)

        expect(api_client).not_to have_received(:create_organization).with(org)

        org.update(api_environments: [0])

        expect(api_client).to have_received(:create_organization).with(org)
      end

      it 'sends create command if org is created with an env' do
        api_client = instance_double(APIClient)
        allow(APIClient).to receive(:new).with(0).and_return(api_client)
        allow(api_client).to receive(:create_organization)

        org = create(:organization, api_environments: [0])

        expect(api_client).to have_received(:create_organization).with(org)
      end

      it 'sends delete and create commands if both new and removed env' do
        org = build(:organization, api_environments: [0])

        api_client_0 = instance_double(APIClient)
        api_client_1 = instance_double(APIClient)
        allow(APIClient).to receive(:new).with(0).and_return(api_client_0)
        allow(APIClient).to receive(:new).with(1).and_return(api_client_1)
        allow(api_client_0).to receive(:create_organization).with(org)
        allow(api_client_0).to receive(:delete_organization).with(org)
        allow(api_client_1).to receive(:create_organization).with(org)

        # Finish creation
        org.save

        # Make the big change
        expect(api_client_0).to have_received(:create_organization).with(org)

        org.update(api_environments: [1])

        expect(api_client_1).to have_received(:create_organization).with(org)
        expect(api_client_0).to have_received(:delete_organization).with(org)
      end
    end
  end
end
