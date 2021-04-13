# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'new user signs up for account' do

  before(:each) do
    visit new_user_session_path
    click_link 'sign-up'
  end

  context 'when successful' do
    scenario 'create an account and sign out' do
      fill_in :user_first_name, with: 'Samuel'
      fill_in :user_last_name, with: 'Vimes'
      fill_in :user_email, with: 'vimes@example.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_implementer, with: 'Night Watch Clinic'
      check :user_agree_to_terms
      click_on('Sign up')

      expect(page).to have_content('Samuel Vimes')

      find('#sign-out', visible: false).click

      expect(page).to have_content('Log in')
      expect(page).to have_content('Request access')
    end
  end

  context 'when unsuccessful' do
    scenario 'returns errors for missing fields' do
      click_on('Sign up')

      expect(page).to have_content('7 errors prohibited this user from being saved:')
      expect(page).to have_content("Email can't be blank")
      expect(page).to have_content("Password can't be blank")
      expect(page).to have_content("First name can't be blank")
      expect(page).to have_content("Last name can't be blank")
      expect(page).to have_content("Implementer can't be blank")
      expect(page).to have_content('Password must include at least one number, one lowercase letter, one uppercase letter, and one special character (!@#$&*-)')
      expect(page).to have_content('Agree to terms you must agree to the terms of service to create an account')
    end
  end
end
