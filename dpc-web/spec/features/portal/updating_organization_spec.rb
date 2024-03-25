# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'


RSpec.feature 'updating my organization' do
  include OrganizationsHelper

  let!(:user) { create :user, :assigned }

  before(:each) do
    sign_in user, scope: :user
  end

  scenario 'updating the org with a vailid Npi' do
    npi = LuhnacyLib.generate_npi

    visit root_path
    find('[data-test="edit-link"]').click
    fill_in 'organization_npi', with: npi
    fill_in 'organization_vendor', with: 'Cool EMR Vendor Name'
    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page).to have_content('Organization updated.')
    expect(page.body).to have_content(npi)
    expect(page.body).to have_content('Cool EMR Vendor Name')
  end

  scenario 'updating the org with an invalid Npi' do
    visit root_path
    find('[data-test="edit-link"]').click
    fill_in 'organization_npi', with: '123456789'
    find('[data-test="form-submit"]').click

    expect(page).to have_css('[data-test="form-submit"]')
    expect(page).to have_content('Organization could not be updated: Npi must be valid.')
  end
end
