require 'rails_helper'

RSpec.feature 'user deletes account' do
  let(:user) { create :user }

  def user_sign_in
    visit new_user_session_path
    fill_in 'user_email', with: user.email
    fill_in 'user_password', with: '123456'
    find('[data-test="submit"]').click
    visit edit_user_registration_path
  end

  context 'when successful' do
    scenario 'user inputs correct password' do
      user_sign_in
      fill_in 'user_password_to_delete', with: '123456'

      find('[data-test="delete-user-account"]').click

      expect(User.last).not_to be_present
    end
  end

  context 'when unsuccessful' do
    scenario 'user inputs incorrect password' do
      user_sign_in
      fill_in 'user_password_to_delete', with: '3v3ryDayPotato'

      find('[data-test="delete-user-account"]').click

      expect(User.last).to be_present
    end

    scenario 'user inputs no password' do
      user_sign_in
      fill_in 'user_password_to_delete', with: nil

      find('[data-test="delete-user-account"]').click

      expect(User.last).to be_present
    end
  end
end