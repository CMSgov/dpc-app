# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'creating and updating organizations' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    stub_creation_request
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully creating and updating an organization\'s attributes', :perform_enqueued do
    visit new_internal_organization_path

    fill_in 'organization_name', with: 'Good Health'
    select 'Primary Care Clinic', from: 'organization_organization_type'
    fill_in 'organization_num_providers', with: '2200'

    select 'Temp', from: 'organization_address_attributes_address_use'
    select 'Both', from: 'organization_address_attributes_address_type'
    fill_in 'organization_address_attributes_street', with: '1 North Main'
    fill_in 'organization_address_attributes_street_2', with: 'Ste 2000'
    fill_in 'organization_address_attributes_city', with: 'Greenville'
    select 'South Carolina', from: 'organization_address_attributes_state'
    fill_in 'organization_address_attributes_zip', with: '29601'

    fill_in 'organization_fhir_endpoints_attributes_0_name', with: 'Provider Endpoint'
    fill_in 'organization_fhir_endpoints_attributes_0_uri', with: 'https://FhirEndpoint.example.com'
    fill_in 'organization_npi', with: '555ttt444'
    select 'Test', from: 'organization_fhir_endpoints_attributes_0_status'

    check 'organization_api_environments_sandbox'

    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')

    expect(page.body).to have_content('Good Health')
    expect(page.body).to have_content('2200')
    expect(page.body).to have_content('Primary Care Clinic')
    expect(page.body).to have_content('1 North Main')
    expect(page.body).to have_content('Sandbox')
    expect(page.body).to have_content('Provider Endpoint')
    expect(page.body).to have_content('https://FhirEndpoint.example.com')
    expect(page.body).to have_content('Test')

    find('[data-test="edit-link"]').click

    fill_in 'organization_name', with: 'Health Revisited'
    select 'Multispecialty Clinic', from: 'organization_organization_type'
    uncheck 'organization_api_environments_sandbox'
    fill_in 'organization_address_attributes_street', with: '50 River St'
    select 'Off', from: 'organization_fhir_endpoints_attributes_0_status'
    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Health Revisited')
    expect(page.body).to have_content('Multispecialty Clinic')
    expect(page.body).to have_content('50 River St')
    expect(page.body).to have_content('Off')
    expect(page.body).not_to have_content('Sandbox')
  end

  scenario 'trying to update a organization with invalid attributes ' do
    org = create(:organization, name: 'Good Health')

    visit edit_internal_organization_path(org)

    expect(page.body).to have_content('Good Health')

    fill_in 'organization_name', with: ''

    find('[data-test="form-submit"]').click

    # Still on edit page
    expect(page).to have_css('[data-test="form-submit"]')
  end

  def stub_creation_request
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')
    stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
      headers: { 'Content-Type' => 'application/json', 'Authorization' => 'Bearer 112233' },
      body: {
        resourceType: 'Parameters',
        parameter: [{
          name: 'resource',
          resource: {
            resourceType: 'Bundle',
            type: 'collection',
            entry: [{
              resource: {
                address: [{
                  use: 'temp',
                  type: 'both',
                  city: 'Greenville',
                  country: 'US',
                  line: ['1 North Main', 'Ste 2000'],
                  postalCode: '29601',
                  state: 'SC'
                }],
                identifier: [{system: 'http://hl7.org/fhir/sid/us-npi', value: '555ttt444'}],
                name: 'Good Health',
                resourceType: 'Organization',
                type: [{
                  coding: [{
                    code: 'prov', display: 'Healthcare Provider', system: 'http://hl7.org/fhir/organization-type'
                  }],
                  text: 'Healthcare Provider'
                }]
              }
            }, {
              resource: {
                resourceType: 'Endpoint',
                status: 'test',
                connectionType: {system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type', code: 'hl7-fhir-rest'},
                name: 'Provider Endpoint', address: 'https://FhirEndpoint.example.com'
              }
            }]
          }
        }]
      }.to_json
    ).to_return(
      status: 200,
      body: "{\"id\":\"8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d\",\"endpoint\":[{\"reference\":\"Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66\"}]}"
    )
  end
end