# frozen_string_literal: true

require './spec/shared_examples/authenticable_page'

RSpec.feature 'internal user signs in' do
  let(:internal_user) { create :internal_user, password: '123456', password_confirmation: '123456' }

  scenario 'the internal user logs in' do
    visit new_internal_user_session_path
    fill_in 'internal_user_email', with: internal_user.email
    fill_in 'internal_user_password', with: '123456'
    find('[data-test="submit"]').click

    expect(page).to have_css('[data-test="internal-user-signout"]')
  end

  scenario 'internal user cannot sign in and then sign in as user' do
    visit new_internal_user_session_path
    fill_in 'internal_user_email', with: internal_user.email
    fill_in 'internal_user_password', with: '123456'
    find('[data-test="submit"]').click

    expect(page).to have_css('[data-test="internal-user-signout"]')

    visit new_user_session_path

    expect(page).not_to have_css('[data-test="user-sign-in-form"]')
    expect(page).to have_css('[data-test="internal-user-signout"]')
  end
end