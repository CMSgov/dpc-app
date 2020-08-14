# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user resets password' do
  let(:user) { create :user }

  context 'when successful' do
    scenario 'user resets password from recovery email' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:3000(?<path>.+?)">})[:path]

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
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(%r{href="http:\/\/localhost:3000(?<path>.+?)">})[:path]

      visit reset_link

      fill_in 'user_password', with: 'CrabW0rd$_B00m#'
      fill_in 'user_password_confirmation', with: "CrabW0rd$_B00m!"
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password confirmation doesn&#39;t match Password')

      fill_in 'user_password', with: '1Crab'
      fill_in 'user_password_confirmation', with: '1Crab'
      find('input[data-test="submit"]').click

      expect(page.body).to include('1 error prohibited this user from being saved:')
      expect(page.body).to include('Password is too short (minimum is 6 characters)')

      fill_in 'user_password', with: 'CrabW0rd$_B00m#'
      fill_in 'user_password_confirmation', with: 'CrabW0rd$_B00m#'
      find('input[data-test="submit"]').click

      expect(page.body).to include('Your password has been changed successfully')
    end
  end
end
