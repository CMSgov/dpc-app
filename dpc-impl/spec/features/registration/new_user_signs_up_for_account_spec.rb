# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'new user signs up for account' do
  include ApiClientSupport
  include MailerHelper

  before(:each) do
    visit new_user_session_path
    click_link 'sign-up'
  end

  context 'when successful' do
    before(:each) do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      fill_in :user_first_name, with: 'Samuel'
      fill_in :user_last_name, with: 'Vimes'
      fill_in :user_email, with: 'vimes@gmail.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_implementer, with: 'Surreal Kayak'
      check :user_agree_to_terms

      click_on('Sign up')
    end

    scenario 'the email entered in to our job queue' do

      expect(ActionMailer::Base.deliveries.count).to eq(1)

      visit new_user_session_path
      click_link 'sign-up'
      fill_in :user_first_name, with: 'Angua'
      fill_in :user_last_name, with: 'Überwald'
      fill_in :user_email, with: 'angua@gmail.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_implementer, with: 'Surreal Kayak'
      check :user_agree_to_terms
      
      click_on('Sign up')

      expect(ActionMailer::Base.deliveries.count).to eq(2)
    end

    scenario 'user sent a confirmation email with confirmation token' do
      Sidekiq::Worker.drain_all
      expect(:confirmation_token).to be_present

      ctoken = last_email.body.match(/confirmation_token=[^"]*/)

      expect(ctoken).to be_present
    end

    scenario 'user clicks on confirmation link to navigate to portal' do
      Sidekiq::Worker.drain_all
      ctoken = last_email.body.match(/confirmation_token=[^"]*/)

      visit "/users/confirmation?#{ctoken}"

      expect(page).to have_http_status(200)
      expect(page).to have_content('Welcome Samuel Vimes')
    end
  end

  context 'when missing required fields on form' do
    scenario 'returns to the log in page with error messages' do
      click_on('Sign up')

      expect(page).to have_content("Email can't be blank")
      expect(page).to have_content("Password can't be blank")
      expect(page).to have_content("First name can't be blank")
      expect(page).to have_content("Last name can't be blank")
      expect(page).to have_content("Implementer can't be blank")
      expect(page).to have_content('Email is invalid')
      expect(page).to have_content('Password must include at least one number, one lowercase letter, one uppercase letter, and one special character (!@#$&*-)')
      expect(page).to have_content('Agree to terms you must agree to the terms of service to create an account')
    end
  end

  context 'when using an email already registered' do
    before(:each) do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)
    end

    scenario 'returns to the sign in page with error' do
      create(:user, email: 'vimes@gmail.com')

      fill_in :user_first_name, with: 'Samuel'
      fill_in :user_last_name, with: 'Vimes'
      fill_in :user_email, with: 'vimes@gmail.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_implementer, with: 'Surreal Kayak'
      check :user_agree_to_terms

      click_on('Sign up')

      expect(page).to have_content('Email has already been taken')
    end
  end

  context 'when a user has not verified their email' do
    scenario 'unverified user tries and fails to sign in' do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      fill_in :user_first_name, with: 'Samuel'
      fill_in :user_last_name, with: 'Vimes'
      fill_in :user_email, with: 'vimes@gmail.com'
      fill_in :user_password, with: '3veryDay#P0tato'
      fill_in :user_password_confirmation, with: '3veryDay#P0tato'
      fill_in :user_implementer, with: 'Surreal Kayak'
      check :user_agree_to_terms

      click_on('Sign up')

      Sidekiq::Worker.drain_all

      visit new_user_session_path

      last_user = User.last

      fill_in 'user_email', with: last_user.email
      fill_in 'user_password', with: '3veryDay#P0tato'

      find('[data-test="submit"]').click

      expect(last_user.confirmed_at).to be_nil
      expect(page).to have_content('Log in')
    end
  end
end
