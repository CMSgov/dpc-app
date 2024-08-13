require 'rails_helper'

RSpec.feature 'user deletes account' do
  let(:user) { create :user }

  before(:each) do
    sign_in user, scope: :user
    visit edit_user_registration_path
  end

  context 'when successful' do
    scenario 'user inputs correct password' do
      fill_in 'user_password_to_delete', with: '12345ABCDEfghi!'

      find('[data-test="delete-user-account"]').click

      expect(page.body).to include('Bye! Your account has been successfully cancelled. We hope to see you again soon.')
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
