# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  describe 'callbacks' do
    describe '#update_registered_organizations' do
      context 'with NPI' do
        it 'kicks off OrganizationRegistrar after save', :perform_enqueued do
          allow(OrganizationRegistrar).to receive(:run)

          org = create(:organization, api_environments: [0], npi: SecureRandom.uuid)

          expect(OrganizationRegistrar).to have_received(:run).
            with(organization: org, api_environments: ['sandbox'])

          org.update(api_environments: [1])

          expect(OrganizationRegistrar).to have_received(:run).
            with(organization: org, api_environments: ['production'])
        end
      end

      context 'without NPI' do
        it 'does not kick off OrganizationRegistrar', :perform_enqueued do
          allow(OrganizationRegistrar).to receive(:run)
          org = create(:organization, npi: nil)
          org.update(api_environments: [1])

          expect(OrganizationRegistrar).not_to have_received(:run).
            with(organization: org, api_environments: ['production'])
        end
      end
    end
  end

  describe 'validations' do
    describe 'api_environments inclusion' do
      context 'api_environments not in registered organization enum' do
        it 'is invalid' do
          org = build(:organization, api_environments: [0,500])
          expect(org).not_to be_valid
        end
      end

      context 'api_environments in registered organization enum' do
        it 'is valid' do
          org = build(:organization, api_environments: [0,1])
          expect(org).to be_valid
        end
      end

      context 'with no api_environments' do
        it 'is valid' do
          org = build(:organization, api_environments: [])
          expect(org).to be_valid
        end
      end
    end
  end

  describe '#api_environments=' do
    it 'replaces non-array input with array' do
      org = create(:organization, api_environments: 'not_array')

      expect(org.api_environments).to eq([])
    end

    it 'removes blanks and turns strings to integers in array input' do
      org = create(:organization, api_environments: ['', nil, '1'])

      expect(org.api_environments).to eq([1])
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
    it 'returns array of matching env strings' do
      org = build(:organization, api_environments: [0])

      expect(org.api_environment_strings).to match_array(['sandbox'])
    end

    it 'returns empty array if no matching envs' do
      org = build(:organization, api_environments: [])

      expect(org.api_environment_strings).to match_array([])
    end
  end

  describe '#api_credentialable?' do
    it 'returns true if org has a registered org and an npi' do
      org = create(:organization, api_environments: [0], npi: SecureRandom.uuid)
      create(:registered_organization, organization: org)

      expect(org.api_credentialable?).to be true
    end

    it 'returns false if npi present but no registered org' do
      org = create(:organization, api_environments: [0], npi: SecureRandom.uuid)

      expect(org.api_credentialable?).to be false
    end

    it 'returns false if registered org present but no npi' do
      org = create(:organization, api_environments: [0], npi: nil)
      create(:registered_organization, organization: org)

      expect(org.api_credentialable?).to be false
    end

    it 'returns false if no npi or registered org' do
      org = create(:organization, api_environments: [], npi: nil)

      expect(org.api_credentialable?).to be false
    end
  end
end
