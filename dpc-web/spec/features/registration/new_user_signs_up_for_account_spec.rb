# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'new user signs up for account' do
  before(:each) do
    visit new_user_session_path
    click_link 'sign-up'
  end

  context 'when successful' do
    scenario 'is redirected to dashboard and can see profile' do
      fill_in :user_first_name, with: 'Clarissa'
      fill_in :user_last_name, with: 'Dalloway'
      fill_in :user_email, with: 'clarissa@example.com'
      fill_in :user_password, with: '1234567890'
      fill_in :user_password_confirmation, with: '1234567890'
      fill_in :user_requested_organization, with: 'London Health System'
      select 'Primary Care Clinic', from: :user_requested_organization_type
      fill_in :user_requested_num_providers, visible: false, with: '777'
      fill_in :user_address_1, with: '1 Hampton Heath Drive'
      fill_in :user_address_2, with: 'Suite 5'
      fill_in :user_city, with: 'London'
      select 'New York', from: :user_state
      fill_in :user_zip, with: '10033'
      check :user_agree_to_terms

      click_on('Sign up')

      expect(page).to have_http_status(200)
      expect(page).to have_css('[data-test="my-account-menu"]')

      find('[data-test="my-account-menu"]').click
      find('[data-test="dpc-registrations-profile-link"]', visible: false).click

      email_field = find('#user_email')
      expect(email_field.value).to eq('clarissa@example.com')
    end
  end

  context 'when not agreeing to terms' do
    scenario 'returns to the sign in page' do
      fill_in :user_first_name, with: 'Clarissa'
      fill_in :user_last_name, with: 'Dalloway'
      fill_in :user_email, with: 'clarissa@example.com'
      fill_in :user_password, with: '1234567890'
      fill_in :user_password_confirmation, with: '1234567890'
      fill_in :user_requested_organization, with: 'London Health System'
      select 'Primary Care Clinic', from: :user_requested_organization_type
      fill_in :user_requested_num_providers, visible: false, with: '777'
      fill_in :user_address_1, with: '1 Hampton Heath Drive'
      fill_in :user_address_2, with: 'Suite 5'
      fill_in :user_city, with: 'London'
      select 'New York', from: :user_state
      fill_in :user_zip, with: '10033'
      fill_in :user_zip, with: '10033'

      uncheck :user_agree_to_terms

      click_on('Sign up')

      expect(page).to have_content('Log in')
      expect(page).to have_content('you must agree')
    end
  end

  context 'when missing information on form' do
    scenario 'returns to the sign in page with error message' do
      fill_in :user_first_name, with: 'Clarissa'
      fill_in :user_email, with: 'clarissa@example.com'
      fill_in :user_password, with: '1234567890'
      fill_in :user_password_confirmation, with: '1234567890'
      fill_in :user_requested_organization, with: 'London Health System'
      select 'Primary Care Clinic', from: :user_requested_organization_type
      fill_in :user_requested_num_providers, visible: false, with: '777'
      fill_in :user_address_1, with: '1 Hampton Heath Drive'
      fill_in :user_address_2, with: 'Suite 5'
      fill_in :user_city, with: 'London'
      select 'New York', from: :user_state
      fill_in :user_zip, with: '10033'
      check :user_agree_to_terms

      click_on('Sign up')

      expect(page).to have_content('Log in')
      expect(page).to have_content("Last name can't be blank")
    end
  end

  context 'when using an email already registered' do
    scenario 'returns to the sign in page with error' do
      create(:user, email: 'clarissa@example.com')

      fill_in :user_first_name, with: 'Clarissa'
      fill_in :user_last_name, with: 'Dalloway'
      fill_in :user_email, with: 'clarissa@example.com'
      fill_in :user_password, with: '1234567890'
      fill_in :user_password_confirmation, with: '1234567890'
      fill_in :user_requested_organization, with: 'London Health System'
      select 'Primary Care Clinic', from: :user_requested_organization_type
      fill_in :user_requested_num_providers, visible: false, with: '777'
      fill_in :user_address_1, with: '1 Hampton Heath Drive'
      fill_in :user_address_2, with: 'Suite 5'
      fill_in :user_city, with: 'London'
      select 'New York', from: :user_state
      fill_in :user_zip, with: '10033'
      check :user_agree_to_terms

      click_on('Sign up')

      expect(page).to have_content('Log in')
      expect(page).to have_content('Email has already been taken')
    end
  end
end
