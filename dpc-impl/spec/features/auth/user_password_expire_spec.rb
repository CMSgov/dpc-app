# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user password expires after 2 months' do
  include ApiClientSupport

  let(:user) { create :user, password: '12345ABCDEfghi!', password_confirmation: '12345ABCDEfghi!', password_changed_at: 61.days.ago }

  context 'when successful' do

    before(:each) do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      api_client = instance_double(ApiClient)
      allow(ApiClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:get_provider_orgs)
        .with(user.implementer_id)
        .and_return(api_client)
      allow(api_client).to receive(:response_successful?).and_return(false)
      allow(api_client).to receive(:response_body).and_return(nil)
    end

    scenario 'user is redirected after password expires' do
      visit new_user_session_path
  
      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: '12345ABCDEfghi!'
      find('[data-test="submit"]').click
  
      expect(page.body).to include('Renew your password')

      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: 'N0#M0urn3r$#N0#Fun3ral$'
      fill_in 'user_password_confirmation', with: 'N0#M0urn3r$#N0#Fun3ral$'
      find('[data-test="submit"]').click

      expect(page.body).to include('Your new password is saved.')
      expect(page.body).to include("Welcome #{user.name}")
    end
  end

  context 'when unsuccessful' do
    scenario 'user inputs invalid passwords' do
      visit new_user_session_path

      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: '12345ABCDEfghi!'
      find('[data-test="submit"]').click

      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: 'everydaypotatotomorrow'
      fill_in 'user_password_confirmation', with: 'everydaypotatotomorrow'
      find('[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password must include at least one number, one lowercase letter')
      expect(page.body).to include('one uppercase letter, and one special character (!@#$&amp;*-)')


      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: '#1Tater'
      fill_in 'user_password_confirmation', with: '#1Tater'
      find('[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password is too short (minimum is 15 characters)')

      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: '3v3ryDay*P0tat0'
      fill_in 'user_password_confirmation', with: '3v3ryDay*P0tato'
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password confirmation doesn&#39;t match Password')

      fill_in 'user_password', with: '3v3ryDay*P0tat0'
      fill_in 'user_password_confirmation', with: '3v3ryDay*P0tat0'
      find('input[data-test="submit"]').click

      expect(page.body).to include('3 errors prohibited this user from being saved:')
      expect(page.body).to include('Current password can&#39;t be blank')
      expect(page.body).to include('Password is invalid')
      expect(page.body).to include('Password confirmation is invalid')
    end

    scenario 'user tries to update with previously used password' do
      visit new_user_session_path

      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: '12345ABCDEfghi!'
      find('[data-test="submit"]').click


      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: '12345ABCDEfghi!'
      fill_in 'user_password_confirmation', with: '12345ABCDEfghi!'
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password cannot match previously used password.')
    end
  end
end