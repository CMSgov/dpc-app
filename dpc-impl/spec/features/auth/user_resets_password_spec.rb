# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user resets password' do
  include ApiClientSupport

  let(:user) { create :user, password: '12345ABCDEfghi!', password_confirmation: '12345ABCDEfghi!' }

  context 'when successful' do
    before(:each) do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      api_client = instance_double(ApiClient)
      allow(ApiClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:get_client_orgs)
        .with(user.implementer_id)
        .and_return(api_client)
      allow(api_client).to receive(:response_successful?).and_return(false)
      allow(api_client).to receive(:response_body).and_return(nil)
    end

    scenario 'user resets password from recovery email' do
      visit new_user_password_path

      expect(page.body).to have_content('Forgot your password?')

      fill_in 'user_email', with: user.email
      find('input[data-test="submit"]').click

      expect(ActionMailer::Base.deliveries.count).to eq(1)

      last_delivery = ActionMailer::Base.deliveries.last
      email = last_delivery.body

      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:4000(?<path>.+?)">})[:path]

      visit reset_link

      expect(page.body).to include("Change your password")

      fill_in 'user_password', with: '3v3ryDayP0tat0!'
      fill_in 'user_password_confirmation', with: '3v3ryDayP0tat0!'
      find('input[data-test="submit"]').click

      expect(page.body).to include('Your password has been changed successfully')

      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: '3v3ryDayP0tat0!'
      find('input[data-test="submit"]').click

      expect(page.body).to include("Welcome #{user.name}")
    end
  end

  context 'when unsuccessful' do
    scenario 'user inputs invalid passwords before successful reset' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email
      find('input[data-test="submit"]').click

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:4000(?<path>.+?)">})[:path]

      visit reset_link

      fill_in 'user_password', with: 'CrabW0rd$_B00m#'
      fill_in 'user_password_confirmation', with: "CrabW0rd$_B00m!"
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password confirmation doesn&#39;t match Password')

      fill_in 'user_password', with: '#1Crab'
      fill_in 'user_password_confirmation', with: '#1Crab'
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password is too short (minimum is 15 characters)')

      fill_in 'user_password', with: 'CrabwardTentacles'
      fill_in 'user_password_confirmation', with: 'CrabwardTentacles'
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password must include at least one number, one lowercase letter')
      expect(page.body).to include('one uppercase letter, and one special character (!@#$&amp;*-)')

      fill_in 'user_password', with: 'CrabW0rd$_B00m#'
      fill_in 'user_password_confirmation', with: 'CrabW0rd$_B00m#'
      find('input[data-test="submit"]').click

      expect(page.body).to include('Your password has been changed successfully')
    end

    scenario 'user inputs old password before successful reset' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email
      find('input[data-test="submit"]').click

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:4000(?<path>.+?)">})[:path]

      visit reset_link

      expect(page.body).to include("Change your password")

      fill_in 'user_password', with: '12345ABCDEfghi!'
      fill_in 'user_password_confirmation', with: '12345ABCDEfghi!'
      find('input[data-test="submit"]').click

      expect(page.body).to include("Password cannot match previously used password.")

      fill_in 'user_password', with: 'CrabW0rd$_B00m#'
      fill_in 'user_password_confirmation', with: 'CrabW0rd$_B00m#'
      find('input[data-test="submit"]').click

      expect(page.body).to include('Your password has been changed successfully')
    end
  end

  context 'too many emails' do
    around(:each) do |spec|
      default_limit = Rails.configuration.x.mail_throttle.limit
      Rails.configuration.x.mail_throttle.limit = 1
      spec.run
      Rails.configuration.x.mail_throttle.limit = default_limit
    end

    scenario 'it does not send an email' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(0)
    end
  end
end
