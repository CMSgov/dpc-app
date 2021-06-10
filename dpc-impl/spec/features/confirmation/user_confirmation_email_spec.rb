# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user resends confirmation instructions' do
  include ApiClientSupport

  let(:user) { create :user, confirmed_at: nil }

  context 'when successful' do
    scenario 'user resends confirmation instructions' do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      api_client = instance_double(ApiClient)
      allow(ApiClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:get_provider_orgs)
        .with(user.implementer_id)
        .and_return(api_client)
      allow(api_client).to receive(:response_successful?).and_return(false)
      allow(api_client).to receive(:response_body).and_return(nil)

      visit new_user_confirmation_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      email = last_delivery.body

      expect(email).to include('Confirm my account')

      ctoken = email.raw_source.match(/confirmation_token=[^"]*/)

      visit "/impl/users/confirmation?#{ctoken}"

      expect(page.body).to include("Welcome #{user.name}")
    end
  end

  context 'when requesting to resend confirmation with incorrect email' do
    scenario 'error does not indicate if email is in the database' do
      visit new_user_confirmation_path

      fill_in 'user_email', with: 'thisIsATest@gmail.com'

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
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      visit new_user_confirmation_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
        Sidekiq::Worker.drain_all
      end.to change(ActionMailer::Base.deliveries, :count).by(0)
    end
  end
end