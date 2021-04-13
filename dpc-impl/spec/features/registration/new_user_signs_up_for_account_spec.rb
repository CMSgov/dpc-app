# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'new user signs up for account' do

  before(:each) do
    visit new_user_session_path
    click_link 'sign-up'
  end

  context 'when successful' do
    before(:each) do
      fill_in :user_first_name, with: 'Clarissa'
      fill_in :user_last_name, with: 'Dalloway'
      fill_in :user_email, with: 'clarissa@example.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_requested_organization, with: 'London Health System'
      select 'Primary Care Clinic', from: :user_requested_organization_type
      fill_in :user_requested_num_providers, visible: false, with: '777'
      fill_in :user_address_1, with: '1 Hampton Heath Drive'
      fill_in :user_address_2, with: 'Suite 5'
      fill_in :user_city, with: 'London'
      select 'New York', from: :user_state
      fill_in :user_zip, with: '10033'
      check :user_agree_to_terms
    end

    scenario 'create an account' do
      click_on('Sign up')

      expect(page).to have_content('Welcome Clarissa Dalloway')
    end
  end

  context 'when unsuccessful' do
  end
end
