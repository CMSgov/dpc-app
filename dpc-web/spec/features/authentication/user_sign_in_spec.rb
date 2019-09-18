# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user signs in' do
  let!(:user) { create :user, password: '123456', password_confirmation: '123456' }
  let!(:dpc_registration) { create :dpc_registration, user: user }

  scenario 'when successful' do
    visit new_user_session_path
    fill_in 'user_email', with: user.email
    fill_in 'user_password', with: '123456'
    find('[data-test="submit"]').click

    expect(page).to have_css('[data-test="my-account-menu"]')
  end

  scenario 'user cannot then sign in as internal user' do
    visit new_user_session_path
    fill_in 'user_email', with: user.email
    fill_in 'user_password', with: '123456'
    find('[data-test="submit"]').click

    expect(page).to have_css('[data-test="my-account-menu"]')

    visit new_internal_user_session_path

    expect(page).not_to have_css('[data-test="internal-user-sign-in-form"]')
    expect(page).to have_css('[data-test="my-account-menu"]')
  end
end
