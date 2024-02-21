# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user resends confirmation instructions' do
  let(:user) { create :user, confirmed_at: nil }

  context 'when successful' do
    scenario 'user resends confirmation instructions' do
      visit new_user_confirmation_path

      fill_in 'user_email', with: user.email

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

  context 'when incorrect email' do
    scenario 'user cannot request to resend confirmation' do
      visit new_user_confirmation_path

      fill_in 'user_email', with: 'not_a_real@email.com'

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(0)

      expect(page.body).to include('If your email address exists in our database, you will receive an email with instructions for how to confirm your email address in a few minutes.')
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
      visit new_user_confirmation_path

      fill_in 'user_email', with: user.email

      # The initial confirmation email when creating a new user does not count
      # towards the mailing threshold (as this email can only ever be sent once anyways)
      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(2)

      visit new_user_confirmation_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(0)
    end
  end
end
