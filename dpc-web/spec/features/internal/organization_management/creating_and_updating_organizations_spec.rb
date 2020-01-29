# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'creating and updating organizations' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully creating and updating an organization\'s attributes' do
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

    fill_in 'organization_vendor', with: 'Cool EMR Vendor'
    fill_in 'organization_npi', with: '555ttt444'

    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')

    expect(page.body).to have_content('Good Health')
    expect(page.body).to have_content('2200')
    expect(page.body).to have_content('Primary Care Clinic')
    expect(page.body).to have_content('1 North Main')
    expect(page.body).to have_content('Cool EMR Vendor')

    find('[data-test="edit-link"]').click

    new_name = 'Health Revisited'
    reg_org = Organization.find_by(name: 'Good Health').registered_organizations.first

    fill_in 'organization_name', with: new_name
    fill_in 'organization_npi', with: '9987966711'
    select 'Multispecialty Clinic', from: 'organization_organization_type'
    fill_in 'organization_address_attributes_street', with: '50 River St'
    find('[data-test="form-submit"]').click

    # TODO expect api request to have been made

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content(new_name)
    expect(page.body).to have_content('9987966711')
    expect(page.body).to have_content('Multispecialty Clinic')
    expect(page.body).to have_content('50 River St')
  end

  scenario 'trying to update an organization with invalid attributes ' do
    org = create(:organization, name: 'Good Health')

    visit edit_internal_organization_path(org)
    expect(page.body).to have_content('Good Health')

    fill_in 'organization_name', with: ''
    find('[data-test="form-submit"]').click

    # Still on edit page
    expect(page).to have_css('[data-test="form-submit"]')
  end

  scenario 'enabling sandbox access successfully' do
    stub_api_env_variables

    org = create(:organization)
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')
    fishy = create(:user, first_name: 'Fish', last_name: 'Marlin', email: 'fish@beach.com')
    create(:user, first_name: 'Unrelated', last_name: 'User', email: 'unrelated@beach.com')
    org.users << crabby
    org.users << fishy

    stub_creation_request(org)
    mailer = stub_sandbox_notification_mailer(org, [crabby, fishy])

    visit internal_organization_path(org)
    find('[data-test="enable-sandbox"]').click

    expect(page).to have_css('[data-test="new-reg-org-sandbox"]')
    fill_in 'registered_organization_fhir_endpoint_attributes_name', with: 'Test Sandbox Endpoint'
    select 'Test', from: 'registered_organization_fhir_endpoint_attributes_status'
    fill_in 'registered_organization_fhir_endpoint_attributes_uri', with: 'https://example.com'
    find('[data-test="form-submit"]').click

    expect(page).to have_content('Access to sandbox enabled.')
    expect(mailer).to have_received(:organization_sandbox_email).twice
  end

  scenario 'disabling sandbox access successfully' do
    # TODO with separate #delete_organization work
  end

  def stub_api_env_variables
    allow(ENV).to receive(:fetch).with('PROD_API_ACCESS_ENABLED', 'false').and_return('false')
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')
  end

  def stub_creation_request(org)
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('112233')
    stub_request(:post, 'http://dpc.example.com/Organization/$submit').with(
      headers: { 'Content-Type' => 'application/fhir+json', 'Accept' => 'application/fhir+json', 'Authorization' => 'Bearer 112233' },
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
                  use: org.address_use,
                  type: org.address_type,
                  city: org.address_city,
                  country: 'US',
                  line: [org.address_street, org.address_street_2],
                  postalCode: org.address_zip,
                  state: org.address_state
                }],
                identifier: [{system: 'http://hl7.org/fhir/sid/us-npi', value: org.npi}],
                name: org.name,
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
                payloadType: [
                  {
                    "coding": [
                      {
                        "system": "http://hl7.org/fhir/endpoint-payload-type",
                        "code": "any"
                      }
                    ]
                  }
                ],
                name: 'Test Sandbox Endpoint', address: 'https://example.com'
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

  def stub_update_request(reg_org_id, new_name)
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo')
    stub_request(:put, "http://dpc.example.com/Organization/#{reg_org_id}").with(
      headers: { 'Content-Type' => 'application/fhir+json;charset=utf-8', 'Authorization' => /Bearer .*/ },
      body: "{\n  \"id\": \"#{reg_org_id}\",\n  \"identifier\": [\n    {\n      \"system\": \"http://hl7.org/fhir/sid/us-npi\",\n      \"value\": \"555ttt444\"\n    }\n  ],\n  \"name\": \"#{new_name}\",\n  \"address\": {\n    \"use\": \"temp\",\n    \"type\": \"both\",\n    \"line\": [\n      \"50 River St\"\n    ],\n    \"city\": \"Greenville\",\n    \"state\": \"SC\",\n    \"postalCode\": \"29601\",\n    \"country\": \"US\"\n  },\n  \"endpoint\": {\n    \"reference\": \"Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66\"\n  },\n  \"resourceType\": \"Organization\"\n}"
    ).to_return(
      status: 200,
      body: "{\"id\":\"#{reg_org_id}\"}"
    )
    stub_request(:put, "http://dpc.example.com/Endpoint/d385cfb4-dc36-4cd0-b8f8-400a6dea2d66").with(
      headers: { 'Content-Type' => 'application/fhir+json;charset=utf-8', 'Authorization' => /Bearer .*/ },
      body:  "{\n  \"id\": \"d385cfb4-dc36-4cd0-b8f8-400a6dea2d66\",\n  \"status\": \"off\",\n  \"connectionType\": {\n    \"system\": \"http://terminology.hl7.org/CodeSystem/endpoint-connection-type\",\n    \"code\": \"hl7-fhir-rest\"\n  },\n  \"name\": \"Provider Endpoint\",\n  \"managingOrganization\": {\n    \"reference\": \"Organization/8453e48b-0b42-4ddf-8b43-07c7aa2a3d8d\"\n  },\n  \"payloadType\": [\n    {\n      \"coding\": [\n        {\n          \"system\": \"http://hl7.org/fhir/endpoint-payload-type\",\n          \"code\": \"any\"\n        }\n      ]\n    }\n  ],\n  \"address\": \"https://FhirEndpoint.example.com\",\n  \"resourceType\": \"Endpoint\"\n}"
    ).to_return(
      status: 200,
      body: "{\"id\":\"d385cfb4-dc36-4cd0-b8f8-400a6dea2d66\"}"
    )
  end

  def stub_sandbox_notification_mailer(org, users=[])
    mailer = double(UserMailer)
    users.each do |user|
      allow(UserMailer).to receive(:with).with(user: user, organization: org).and_return(mailer)
    end

    allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
    allow(mailer).to receive(:deliver_later)
    mailer
  end
end