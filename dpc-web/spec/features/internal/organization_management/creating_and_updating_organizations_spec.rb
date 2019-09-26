# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'creating and updating organizations' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully creating and updating an organization\'s attributes ' do
    visit new_internal_organization_path

    fill_in 'organization_name', with: 'Good Health'
    select 'Primary Care Clinic', from: 'organization_organization_type'
    fill_in 'organization_address_attributes_street', with: '1 North Main'
    fill_in 'organization_address_attributes_street_2', with: 'Ste 2000'
    fill_in 'organization_address_attributes_city', with: 'Greenville'
    select 'South Carolina', from: 'organization_address_attributes_state'
    fill_in 'organization_address_attributes_zip', with: '29601'

    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Good Health')
    expect(page.body).to have_content('Primary Care Clinic')
    expect(page.body).to have_content('1 North Main')

    find('[data-test="edit-link"]').click

    fill_in 'organization_name', with: 'Health Revisited'
    select 'Multispecialty Clinic', from: 'organization_organization_type'
    fill_in 'organization_address_attributes_street', with: '50 River St'
    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Health Revisited')
    expect(page.body).to have_content('Multispecialty Clinic')
    expect(page.body).to have_content('50 River St')
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