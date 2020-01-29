# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  describe 'callbacks' do
    describe '#update_registered_organizations' do
      # TODO when adding back update API call
      context 'with NPI' do
        xit 'kicks off OrganizationRegistrar after save', :perform_enqueued do
          allow(OrganizationRegistrar).to receive(:run)

          org = create(:organization, :sandbox_enabled, npi: SecureRandom.uuid)

          expect(OrganizationRegistrar).to have_received(:run).
            with(organization: org, api_environments: ['sandbox'])

          org.update(api_environments: [1])

          expect(OrganizationRegistrar).to have_received(:run).
            with(organization: org, api_environments: ['production'])
        end
      end

      context 'without NPI' do
        xit 'does not kick off OrganizationRegistrar', :perform_enqueued do
          allow(OrganizationRegistrar).to receive(:run)
          org = create(:organization, npi: nil)
          org.update(api_environments: [1])

          expect(OrganizationRegistrar).not_to have_received(:run).
            with(organization: org, api_environments: ['production'])
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

  describe '#registered_api_envs' do
    it 'returns array of environments of registered organizations' do
      org = create(:organization)
      create(:registered_organization, organization: org, api_env: 'sandbox')

      expect(org.registered_api_envs).to match_array(['sandbox'])
    end
  end

  describe '#api_environment_strings' do
    it 'returns array of api_envs from registered_organizations' do
      org = create(:organization, :sandbox_enabled)

      expect(org.api_environment_strings).to match_array(['sandbox'])
    end

    it 'returns empty array if no registered_organizations' do
      org = create(:organization)

      expect(org.api_environment_strings).to match_array([])
    end
  end

  describe '#api_credentialable?' do
    it 'returns true if org has a registered org and an npi' do
      org = create(:organization, :sandbox_enabled, npi: SecureRandom.uuid)
      create(:registered_organization, organization: org)

      expect(org.api_credentialable?).to be true
    end

    it 'returns false if npi present but no registered org' do
      org = create(:organization, npi: SecureRandom.uuid)

      expect(org.api_credentialable?).to be false
    end

    it 'returns false if registered org present but no npi' do
      org = create(:organization, :sandbox_enabled, npi: nil)
      create(:registered_organization, organization: org)

      expect(org.api_credentialable?).to be false
    end

    it 'returns false if no npi or registered org' do
      org = create(:organization, npi: nil)

      expect(org.api_credentialable?).to be false
    end
  end

  describe '#notify_users_of_sandbox_access' do
    let!(:organization) { create(:organization) }
    let!(:assignment) { create(:organization_user_assignment, organization: organization) }
    let!(:mailer) { double(UserMailer) }

    before(:each) do
      allow(UserMailer).to receive(:with).and_return(mailer)
      allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
      allow(mailer).to receive(:deliver_later)
    end

    it 'does nothing if sandbox is not enabled' do
      organization.notify_users_of_sandbox_access
      expect(UserMailer).not_to have_received(:with)
    end

    it 'sends org sandbox email to users if sandbox was added' do
      create(:registered_organization, api_env: 'sandbox', organization: organization)
      organization.notify_users_of_sandbox_access

      expect(UserMailer).to have_received(:with)
        .once.with(user: assignment.user, organization: organization)
      expect(mailer).to have_received(:organization_sandbox_email)
    end
  end
end
