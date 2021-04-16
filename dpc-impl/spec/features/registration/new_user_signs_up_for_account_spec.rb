# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'new user signs up for account' do
  include MailerHelper

  before(:each) do
    visit new_user_session_path
    click_link 'sign-up'
  end

  context 'when successful' do
    it 'creates an account and sign out' do
      fill_in :user_first_name, with: 'Samuel'
      fill_in :user_last_name, with: 'Vimes'
      fill_in :user_email, with: 'vimes@gmail.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_implementer, with: 'Night Watch Clinic'
      check :user_agree_to_terms
      click_on('Sign up')

      expect(:confirmation_token).to be_present

      ctoken = last_email.body.match(/confirmation_token=\w*/)

      expect(ctoken).to be_present
  
      ctoken = last_email.body.match(/confirmation_token=\w*/)

      visit "/users/confirmation?#{ctoken}"

      expect(page).to have_content('Samuel Vimes')

      find('#sign-out', visible: false).click

      expect(page).to have_content('Log in')
      expect(page).to have_content('Request access')
    end
  end

  context 'when unsuccessful' do
    it 'returns errors for missing fields' do
      click_on('Sign up')

      expect(page).to have_content('8 errors prohibited this user from being saved:')
      expect(page).to have_content("Email can't be blank")
      expect(page).to have_content("Password can't be blank")
      expect(page).to have_content("First name can't be blank")
      expect(page).to have_content("Last name can't be blank")
      expect(page).to have_content('Email is invalid')
      expect(page).to have_content("Implementer can't be blank")
      expect(page).to have_content('Password must include at least one number, one lowercase letter, one uppercase letter, and one special character (!@#$&*-)')
      expect(page).to have_content('Agree to terms you must agree to the terms of service to create an account')
    end

    scenario 'unverified user tries and fails to sign in' do
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

      visit new_user_session_path

      last_user = User.last

      fill_in 'user_email', with: last_user.email
      fill_in 'user_password', with: '1234567890'
      find('[data-test="submit"]').click

      expect(last_user.confirmed_at).to be_nil
      expect(page).to have_content('Log in')
    end
  end
end
