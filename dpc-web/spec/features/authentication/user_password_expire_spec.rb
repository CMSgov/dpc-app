# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user password expires after 2 months' do
  let(:user) { create :user, password: '12345ABCDEfghi!', password_confirmation: '12345ABCDEfghi!', password_changed_at: 61.days.ago }

  scenario 'user signs in after password expires' do
    visit new_user_session_path

    fill_in 'user_email', with: user.email
    fill_in 'user_password', with: '12345ABCDEfghi!'
    find('[data-test="submit"]').click

    expect(page.body).to include('Renew your password')
    expect(page.body).to include('15 characters minimum')
    expect(page.body).to include('1 lowercase letter')
    expect(page.body).to include('1 uppercase letter')
    expect(page.body).to include('1 number')
    expect(page.body).to include('1 special character (!@#$&*-)')
    expect(page.body).to include('Confirm password')
  end

  context 'user changes password after password expires' do

    scenario 'successful attempt' do
      visit new_user_session_path

      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: '12345ABCDEfghi!'
      find('[data-test="submit"]').click

      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: '3v3ryDay*P0tat0'
      fill_in 'user_password_confirmation', with: '3v3ryDay*P0tat0'
      find('input[data-test="submit"]').click

      expect(page.body).to include('Your new password is saved.')
    end

    scenario 'unsuccessful attempts' do
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

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Current password can&#39;t be blank')

      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      fill_in 'user_password', with: 'Tooshort#1'
      fill_in 'user_password_confirmation', with: 'Tooshort#1'
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password is too short (minimum is 15 characters)')
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
      expect(page.body).to include('Password was used previously.')
    end
  end
end
