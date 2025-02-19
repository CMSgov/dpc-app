# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user resets password' do
  include ActiveJob::TestHelper
  let(:user) { create :user }

  context 'when successful' do
    scenario 'user resets password from recovery email' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        perform_enqueued_jobs
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:3500(?<path>.+?)">})[:path]

      visit reset_link

      fill_in 'user_password', with: 'CrabW0rd$_B00m#'
      fill_in 'user_password_confirmation', with: 'CrabW0rd$_B00m#'
      find('input[data-test="submit"]').click

      expect(page.body).to include('Your password has been changed successfully')
    end
  end

  context 'with unsuccessful attempts' do
    scenario 'user inputs invalid passwords before successful reset' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        perform_enqueued_jobs
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:3500(?<path>.+?)">})[:path]

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
  end

  context 'with too many emails' do
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
        perform_enqueued_jobs
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        perform_enqueued_jobs
      end.to change(ActionMailer::Base.deliveries, :count).by(0)
    end
  end
end
