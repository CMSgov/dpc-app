# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.describe Organization, type: :model do
  include ApiClientSupport
  include OrganizationsHelper

  describe 'callbacks' do
    describe '#ENV=prod-sbx' do
      before(:each) do
        allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')
      end

      describe '#fake_npi' do
        it 'creates fake npi' do
          # stub default value
          allow(ENV).to receive(:[]).and_call_original

          org = create(:organization, npi: nil)
          org.assign_id
          expect(org.npi).to be_present
          expect(org.npi).to start_with('3')
        end

        it 'does sets npi if nil' do
          # stub default value
          allow(ENV).to receive(:[]).and_call_original

          org = create(:organization, npi: nil)
          org.assign_id
          expect(org.npi).to be_present
          expect(org.npi).to start_with('3')
        end

        it 'does not set npi if present' do
          # stub default value
          allow(ENV).to receive(:[]).and_call_original

          npi = LuhnacyLib.generate_npi

          org = create(:organization, npi: npi)
          org.assign_id
          expect(org.npi).to eq(npi)
        end
      end
    end
  end

  describe '#npi=' do
    it 'replaces blank string with nil' do
      org = create(:organization, npi: '')
      expect(org.npi).to be_nil
    end

    it 'does not replace non-blank values' do
      npi = LuhnacyLib.generate_npi
      org = create(:organization, npi: npi)
      expect(org.npi).to eq(npi)
    end
  end

  describe '#registered_organization?' do
    context 'when organization is a provider' do
      it 'returns true if org has a registered org and an npi' do
        npi = LuhnacyLib.generate_npi
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        org = create(:organization, :api_enabled, organization_type: 'primary_care_clinic', npi: npi)

        expect(org.registered_organization).to be_present
      end

      # This should return false -- NPIs need to be required to be enabled in the api
      it 'returns true if registered org present but no npi' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        org = create(:organization, :api_enabled, organization_type: 'primary_care_clinic', npi: nil)

        expect(org.registered_organization).to be_present
      end
    end

    context 'when organization is a vendor' do
      it 'returns true if org has a registered org and an npi' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        org = create(:organization, :api_enabled, organization_type: 'health_it_vendor')

        expect(org.registered_organization).to be_present
      end

      it 'returns true if no registered org' do
        org = create(:organization, organization_type: 'health_it_vendor')

        expect(org.registered_organization).to_not be_present
      end
    end
  end

  describe '#notify_users_of_sandbox_access' do
    let!(:organization) { create(:organization) }
    let!(:mailer) { double(UserMailer) }

    before(:each) do
      allow(ENV).to receive(:[]).and_call_original
      allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')
      allow(UserMailer).to receive(:with).and_return(mailer)
      allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
      allow(mailer).to receive(:deliver_later)
    end

    it 'does nothing if api is not enabled' do
      organization.notify_users_of_sandbox_access
      expect(UserMailer).not_to have_received(:with)
    end

    it 'sends org sandbox email to users if sandbox was added' do
      stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
      create(:registered_organization, organization: organization)

      assignment = create(:organization_user_assignment, organization: organization)
      organization.notify_users_of_sandbox_access

      expect(UserMailer).to have_received(:with)
        .once.with(user: assignment.user, vendor: false)
      expect(mailer).to have_received(:organization_sandbox_email)
    end
  end
end
