# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'updating my organization' do
  let!(:user) { create :user, :assigned }

  before(:each) do
    sign_in user, scope: :user
  end

  scenario 'updating the NPI of the org' do
    visit dashboard_path
    find('[data-test="edit-link"]').click
    fill_in 'organization_npi', with: '23423ddasa'
    fill_in 'organization_vendor', with: 'Cool EMR Vendor Name'
    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page).to have_content('Organization updated.')
    expect(page.body).to have_content('23423ddasa')
    expect(page.body).to have_content('Cool EMR Vendor Name')
  end
end
