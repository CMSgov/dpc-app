require 'rails_helper'

RSpec.feature 'user deletes account' do
  let(:user) { create :user }

  before(:each) do
    sign_in user, scope: :user
    visit edit_user_registration_path
  end

  context 'when successful' do
    scenario 'user inputs correct password' do
      fill_in 'user_password_to_delete', with: '123456'

      find('[data-test="delete-user-account"]').click

      visit new_user_session_path
      expect(page.body).to include('Bye! Your account has been successfully cancelled. We hope to see you again soon.')
    end
  end

  context 'when unsuccessful' do
    scenario 'user inputs incorrect password' do
      fill_in 'user_password_to_delete', with: '3v3ryDayPotato'

      find('[data-test="delete-user-account"]').click

      expect(page.body).to include('Your email or password is incorrect')
    end

    scenario 'user inputs no password' do
      fill_in 'user_password_to_delete', with: nil

      find('[data-test="delete-user-account"]').click

      expect(page.body).to include('Your email or password is incorrect')
    end
  end
end