#frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'enabling and disabling organizations' do
  include APIClientSupport
  
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  describe 'enabling a new organization' do
    scenario 'successful with an npi' do
      stub = stub_api_client(
        message: :create_organization,
        success: true,
        response: default_org_creation_response
      )
      allow(stub).to receive(:get_public_keys).and_return(stub)
      allow(stub).to receive(:response_body).and_return(default_org_creation_response, { 'entities' => [] })

      org = create(:organization, name: 'Good Health')

      visit internal_organization_path(org)
      find('[data-test="enable-org"]').click

      expect(page).to have_css('[data-test="new-reg-org"]')
      fill_in 'registered_organization_fhir_endpoint_attributes_name', with: 'Test Sandbox Endpoint'
      select 'Test', from: 'registered_organization_fhir_endpoint_attributes_status'
      fill_in 'registered_organization_fhir_endpoint_attributes_uri', with: 'https://example.com'
      find('[data-test="form-submit"]').click

      expect(page).to have_content('API ID')
    end

    scenario 'unsuccessful without an npi' do
      stub_api_client(message: :create_organization, success: false, response: { 'issues' => ['Bad Request'] })

      org = create(:organization, name: 'Good Health', npi: nil)

      visit internal_organization_path(org)
      find('[data-test="enable-org"]').click


      expect(page).to have_css('[data-test="new-reg-org"]')
      fill_in 'registered_organization_fhir_endpoint_attributes_name', with: 'Test Sandbox Endpoint'
      select 'Test', from: 'registered_organization_fhir_endpoint_attributes_status'
      fill_in 'registered_organization_fhir_endpoint_attributes_uri', with: 'https://example.com'
      find('[data-test="form-submit"]').click

      expect(page).to have_css('[data-test="new-reg-org"]')
      expect(page).to have_content('Organization NPI missing. NPI required to register in API.')
    end
  end
end
