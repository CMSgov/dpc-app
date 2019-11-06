# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  describe 'callbacks' do
    describe '#update_registered_organizations' do
      it 'kicks off OrganizationRegistrar after save', :perform_enqueued do
        allow(OrganizationRegistrar).to receive(:run)

        org = create(:organization, api_environments: [0])

        expect(OrganizationRegistrar).to have_received(:run).
          with(organization: org, api_environments: ['sandbox'])

        org.update(api_environments: [1])

        expect(OrganizationRegistrar).to have_received(:run).
          with(organization: org, api_environments: ['production'])
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

    describe 'npi presence' do
      context 'when npi is present' do
        it 'is valid without api_environments' do
          org = build(:organization, npi: '111222', api_environments: [])
          expect(org).to be_valid
        end

        it 'is valid with api_environments' do
          org = build(:organization, npi: '111222', api_environments: [0])
          expect(org).to be_valid
        end
      end

      context 'when npi is not present' do
        it 'is valid without api_environments' do
          org = build(:organization, npi: nil, api_environments: [])
          expect(org).to be_valid
        end

        it 'is invalid with api_environments' do
          org = build(:organization, npi: nil, api_environments: [0])
          expect(org).not_to be_valid
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
end
