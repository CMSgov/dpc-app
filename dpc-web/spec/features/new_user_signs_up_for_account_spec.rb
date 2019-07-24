# frozen_string_literal: true

require './spec/shared_examples/authenticable_page'

RSpec.feature 'new user signs up for account' do
  let(:user) { build :user }

  before(:each) do
    visit new_user_session_path
    click_link 'sign-up'
  end

  context 'when successful' do
    before(:each) do
      fill_in :user_first_name, with: user.first_name
      fill_in :user_last_name, with: user.last_name
      fill_in :user_email, with: user.email
      fill_in :user_password, with: user.password
      fill_in :user_password_confirmation, with: user.password_confirmation
      click_on('Sign up')
    end

    scenario 'adds a new record to the users table' do
      expect(page).to have_http_status(200)
      expect(User.find_by(email: user.email)).to_not be_nil
    end
  end

  context 'when missing information on form' do
    before(:each) do
      fill_in :user_first_name, with: user.first_name
      fill_in :user_email, with: user.email
      fill_in :user_password, with: user.password
      fill_in :user_password_confirmation, with: user.password_confirmation
      click_on('Sign up')
    end

    scenario 'returns to the sign in page' do
      expect(page).to have_content('Sign up')
      expect(page).to have_content("Last name can't be blank")
    end
  end

  context 'when using an email already registered' do
    before(:each) do
      user.save
      fill_in :user_first_name, with: user.first_name
      fill_in :user_last_name, with: user.last_name
      fill_in :user_email, with: user.email
      fill_in :user_password, with: user.password
      fill_in :user_password_confirmation, with: user.password_confirmation
      click_on('Sign up')
    end

    scenario 'returns to the sign in page' do
      expect(page).to have_content('Sign up')
      expect(page).to have_content('Email has already been taken')
    end
  end
end
