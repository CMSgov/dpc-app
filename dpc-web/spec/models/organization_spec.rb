# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  describe '#api_environments=' do
    it 'replaces non-array input with array to pass to OrganizationRegistrar' do
      org = create(:organization)

      registrar = instance_double(OrganizationRegistrar)
      allow(OrganizationRegistrar).to receive(:new).with(organization: org, api_environments: []).
        and_return(registrar)
      allow(registrar).to receive(:register_all)

      org.update(api_environments: 'not_array')

      expect(OrganizationRegistrar).to have_received(:new).with(organization: org, api_environments: [])
    end

    it 'passes cleaned array input to OrganizationRegistrar' do
      org = create(:organization)

      registrar = instance_double(OrganizationRegistrar)
      allow(OrganizationRegistrar).to receive(:new).with(organization: org, api_environments: ['sandbox']).
        and_return(registrar)
      allow(registrar).to receive(:register_all)

      org.update(api_environments: ['', nil, 'sandbox'])

      expect(OrganizationRegistrar).to have_received(:new).with(organization: org, api_environments: ['sandbox'])
    end
  end

  describe '#api_environments' do
    it 'returns array of environments of registered organizations' do
      org = create(:organization)
      create(:registered_organization, organization: org, api_env: 'sandbox')

      expect(org.api_environments).to match_array(['sandbox'])
    end
  end
end
