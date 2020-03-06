require 'rails_helper'

RSpec.feature 'user deletes account' do
  let(:user) { create :user }

  def user_sign_in
    visit new_user_session_path
    fill_in 'user_email', with: user.email
    fill_in 'user_password', with: '123456'
    find('[data-test="submit"]').click
  end

  # when user puts in correct password and successfully deletes
  context 'when successful' do
    scenario 'user inputs correct password' do
      visit edit_user_registration_path
      fill_in 'user_password_to_delete', with: '123456'

      # binding.pry

      find('[data-test="delete-user-account"]').click

      expect(user).not_to be_present
    end
  end

  # when user puts in wrong password and fails

  # when user puts in no password and fails
end