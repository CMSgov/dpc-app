# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  include APIClientSupport

  describe 'callbacks' do
    describe '#ENV=prod-sbx' do
      before(:each) do
        allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')
      end

      describe '#assign_sandbox_id' do
        it 'sets sandbox_id' do
          org = create(:organization)
          expect(org.sandbox_id).to be_present
        end

        it 'does sets sandbox_id if nil' do
          org = create(:organization, sandbox_id: nil)
          org.assign_id
          expect(org.sandbox_id).to be_present
        end

        it 'does not set sandbox_id if present' do
          org = create(:organization, sandbox_id: '111111')
          org.assign_id
          expect(org.sandbox_id).to eq('111111')
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
      org = create(:organization, npi: '1234567890')
      expect(org.npi).to eq('1234567890')
    end
  end

  describe '#external_identifier' do
    it 'returns npi if not in prod-sbx' do
      org = create(:organization)
      npi = org.npi

      expect(org.external_identifier).to eq(npi)
    end

    describe '#ENV=prod-sbx' do
      it 'returns sandbox_id' do
        allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')
        org = create(:organization)
        sandbox_id = org.sandbox_id

        expect(org.external_identifier).to eq(sandbox_id)
      end
    end
  end

  describe '#registered_api_envs' do
    it 'returns array of environments of registered organizations' do
      stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
      org = create(:organization)
      create(:registered_organization, organization: org, api_env: 'sandbox')

      expect(org.registered_api_envs).to match_array(['sandbox'])
    end

    it 'returns empty array if no registered_organizations' do
      org = create(:organization)

      expect(org.registered_api_envs).to match_array([])
    end
  end

  describe '#api_credentialable?' do
    context 'when organization is a provider' do
      it 'returns true if org has a registered org and an npi' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        org = create(:organization, :sandbox_enabled, organization_type: 'primary_care_clinic', npi: '1010101010')

        expect(org.api_credentialable?).to be true
      end

      it 'returns true if registered org present but no npi' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        org = create(:organization, :sandbox_enabled, organization_type: 'primary_care_clinic', npi: nil)

        expect(org.api_credentialable?).to be true
      end

      it 'returns false if npi present but no registered org' do
        org = create(:organization, organization_type: 'primary_care_clinic', npi: SecureRandom.uuid)

        expect(org.api_credentialable?).to be false
      end

      it 'returns false if no npi or registered org' do
        org = create(:organization, organization_type: 'primary_care_clinic', npi: nil)

        expect(org.api_credentialable?).to be false
      end
    end

    context 'when organization is a vendor' do
      it 'returns true if org has a registered org and an npi' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        org = create(:organization, :sandbox_enabled, organization_type: 'health_it_vendor')

        expect(org.api_credentialable?).to be true
      end

      it 'returns false if no registered org' do
        org = create(:organization, organization_type: 'health_it_vendor')

        expect(org.api_credentialable?).to be false
      end
    end
  end

  describe '#notify_users_of_sandbox_access' do
    let!(:organization) { create(:organization) }
    let!(:mailer) { double(UserMailer) }

    before(:each) do
      allow(UserMailer).to receive(:with).and_return(mailer)
      allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
      allow(mailer).to receive(:deliver_later)
    end

    it 'does nothing if sandbox is not enabled' do
      create(:organization_user_assignment, organization: organization)
      organization.notify_users_of_sandbox_access
      expect(UserMailer).not_to have_received(:with)
    end

    it 'sends org sandbox email to users if sandbox was added' do
      stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
      create(:registered_organization, api_env: 'sandbox', organization: organization)

      assignment = create(:organization_user_assignment, organization: organization)
      organization.notify_users_of_sandbox_access

      expect(UserMailer).to have_received(:with)
        .once.with(user: assignment.user, vendor: false)
      expect(mailer).to have_received(:organization_sandbox_email)
    end
  end
end
