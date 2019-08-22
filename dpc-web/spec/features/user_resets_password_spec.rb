# frozen_string_literal: true

RSpec.feature 'user resets password' do
  let(:user) { create :user }

  context 'when successful' do
    scenario 'user resets password from recovery email' do
      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(/href="http:\/\/localhost:3000(?<path>.+?)">/)[:path]

      visit reset_link

      fill_in 'user_password', with: "CrabW0rd$"
      fill_in 'user_password_confirmation', with: "CrabW0rd$"
      find('input[data-test="submit"]').click

      expect(page.body).to include("Your password has been changed successfully")
    end
  end

  context 'when unsuccessful' do
    before(:each) do
      visit new_user_password_path

      fill_in 'user_email', with: user.email

      expect do
        find('input[data-test="submit"]').click
      end.to change(ActionMailer::Base.deliveries, :count).by(1)

      last_delivery = ActionMailer::Base.deliveries.last
      reset_link = last_delivery.body.raw_source.match(/href="http:\/\/localhost:3000(?<path>.+?)">/)[:path]

      visit reset_link
    end

    scenario 'user does not input matching password and password_confirmation' do
      fill_in 'user_password', with: "CrabW0rd$"
      fill_in 'user_password_confirmation', with: "Idon'tMatch"
      find('input[data-test="submit"]').click

      expect(page.body).to include("1 error prohibited this user from being saved:")
      expect(page.body).to include("Password confirmation doesn&#39;t match Password")
    end


    scenario 'user does not input valid password' do
      fill_in 'user_password', with: "crab"
      fill_in 'user_password_confirmation', with: "crab"
      find('input[data-test="submit"]').click

      expect(page.body).to include("1 error prohibited this user from being saved:")
      expect(page.body).to include("Password is too short")
    end
  end
end