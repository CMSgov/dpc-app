# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user signs in' do
  let!(:user) { create :user, password: '12345ABCDEfghi!', password_confirmation: '12345ABCDEfghi!' }

  scenario 'when successful' do
    visit new_user_session_path
    fill_in 'user_email', with: user.email
    fill_in 'user_password', with: '12345ABCDEfghi!'
    find('[data-test="submit"]').click

    expect(page).to have_css('[data-test="my-account-menu"]')
  end

  scenario 'user cannot sign in if account not confirmed' do
    unconfirmed = create(:user, confirmed_at: nil)

    visit new_user_session_path
    fill_in 'user_email', with: unconfirmed.email
    fill_in 'user_password', with: '12345ABCDEfghi!'
    find('[data-test="submit"]').click

    expect(page.body).to include('You have to confirm your email address before continuing.')

    visit new_user_confirmation_path

    fill_in 'user_email', with: unconfirmed.email

    expect do
      find('input[data-test="submit"]').click
      Sidekiq::Worker.drain_all
    end.to change(ActionMailer::Base.deliveries, :count).by(2)

    last_delivery = ActionMailer::Base.deliveries.last
    email = last_delivery.body

    expect(email).to include('Confirm my account')

    confirmation_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:3500(?<path>.+?)">})[:path]

    visit confirmation_link

    expect(page.body).to include('Welcome!')
  end
end
