# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user edits profile' do
  let(:user) { create :user, password: '12345ABCDEfghi!', password_confirmation: '12345ABCDEfghi!' }

  before(:each) do
    sign_in user
    visit edit_user_registration_path
  end

  context 'when successful' do
    scenario 'user changes their name' do
      fill_in 'user_first_name', with: 'James'
      fill_in 'user_last_name', with: 'Kirk'
      fill_in 'user_current_password', with: '12345ABCDEfghi!'

      find('[data-test="update-user-account"]').click
      
      expect(page.body).to have_content('Your account has been updated successfully.')
      expect(page.body).to have_content('Welcome James Kirk')
    end

    scenario 'user changes password' do
      new_password = 'Jam3s&K1irk#B3st'

      fill_in 'user_password', with: new_password
      fill_in 'user_password_confirmation', with: new_password
      fill_in 'user_current_password', with: '12345ABCDEfghi!'

      find('[data-test="update-user-account"]').click

      expect(page.body).to have_content('Your account has been updated successfully, but since your password was changed, you need to sign in again')

      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: new_password
      find('[data-test="submit"]').click

      expect(page.body).to have_content("Welcome #{user.name}")
    end
  end

  context 'when unsuccessful' do
    scenario 'user does not input current password' do
      fill_in 'user_first_name', with: 'Aleksander'
      fill_in 'user_last_name', with: 'Keen'

      find('[data-test="update-user-account"]').click

      expect(page.body).to have_content("Current password can't be blank")

      fill_in 'user_current_password', with: '12345ABCDEfghi!'
      find('[data-test="update-user-account"]').click

      expect(page.body).to have_content('Welcome Aleksander Keen')
    end

    scenario 'user cannot update password with previously used password' do
      old_password = '12345ABCDEfghi!'
      new_password = '!@#abcd123EFG$%'

      fill_in 'user_password', with: old_password
      fill_in 'user_password_confirmation', with: old_password
      fill_in 'user_current_password', with: old_password
      find('[data-test="update-user-account"]').click

      expect(page.body).to have_content('Password cannot match previously used password.')

      fill_in 'user_password', with: new_password
      fill_in 'user_password_confirmation', with: new_password
      fill_in 'user_current_password', with: old_password
      find('[data-test="update-user-account"]').click

      expect(page.body).to have_content('Your account has been updated successfully, but since your password was changed, you need to sign in again')
    end
  end
end
