# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'creating and updating organizations' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    api_client = instance_double(APIClient)
    allow(APIClient).to receive(:new).and_return(api_client)
    allow(api_client).to receive(:create_organization)
    allow(api_client).to receive(:delete_organization)

    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully creating and updating an organization\'s attributes ' do
    visit new_internal_organization_path

    fill_in 'organization_name', with: 'Good Health'
    select 'Primary Care Clinic', from: 'organization_organization_type'
    fill_in 'organization_num_providers', with: '2200'
    fill_in 'organization_address_attributes_street', with: '1 North Main'
    fill_in 'organization_address_attributes_street_2', with: 'Ste 2000'
    fill_in 'organization_address_attributes_city', with: 'Greenville'
    select 'South Carolina', from: 'organization_address_attributes_state'
    fill_in 'organization_address_attributes_zip', with: '29601'
    fill_in 'organization_profile_endpoint_attributes_name', with: 'Provider Profile'
    fill_in 'organization_profile_endpoint_attributes_uri', with: 'https://profileendpoint.example.com'
    select 'Hl7 Fhir Msg', from: 'organization_profile_endpoint_attributes_connection_type'
    select 'Off', from: 'organization_profile_endpoint_attributes_status'

    check 'organization_api_environments_0'

    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Good Health')
    expect(page.body).to have_content('2200')
    expect(page.body).to have_content('Primary Care Clinic')
    expect(page.body).to have_content('1 North Main')
    expect(page.body).to have_content('Sandbox')
    expect(page.body).to have_content('Provider Profile')
    expect(page.body).to have_content('https://profileendpoint.example.com')
    expect(page.body).to have_content('Hl7 Fhir Msg')
    expect(page.body).to have_content('Off')

    find('[data-test="edit-link"]').click

    fill_in 'organization_name', with: 'Health Revisited'
    select 'Multispecialty Clinic', from: 'organization_organization_type'
    uncheck 'organization_api_environments_0'
    fill_in 'organization_address_attributes_street', with: '50 River St'
    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Health Revisited')
    expect(page.body).to have_content('Multispecialty Clinic')
    expect(page.body).to have_content('50 River St')
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
end