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
    fill_in 'organization_type', with: 'Primary Care Provider'

    find('[data-test="form-submit"]').click

    # No longer on edit page
    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Good Health')
    expect(page.body).to have_content('Primary Care Provider')

    # Click edit link
    # TODO: finish
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