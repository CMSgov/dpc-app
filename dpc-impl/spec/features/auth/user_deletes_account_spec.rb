# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user deletes account' do
  let(:user) { create :user, password: '12345ABCDEfghi!', password_confirmation: '12345ABCDEfghi!' }

  before(:each) do
    sign_in user
    visit edit_user_registration_path
  end

  context 'when successful' do
    scenario 'user deletes account with password' do
      fill_in 'user_password_to_delete', with: '12345ABCDEfghi!'

      find('[data-test="delete-user-account"]').click

      expect(page.body).to include('User account successfully deleted.')
    end
  end

  context 'when unsuccessful' do
    scenario 'user inputs incorrect password' do
      fill_in 'user_password_to_delete', with: '3v3ryDayPotato'

      find('[data-test="delete-user-account"]').click

      expect(page.body).to include('Current password is invalid')
    end

    scenario 'user inputs no password' do
      fill_in 'user_password_to_delete', with: ''

      find('[data-test="delete-user-account"]').click

      expect(page.body).to include('1 error prohibited this user from being saved')
    end
  end
end
