# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user sends invitation to DPC' do
  include MailerHelper
  let (:user) { create :user }

  context 'when successful' do
    scenario 'user successfully sends invite' do
      sign_in user
      visit new_user_invitation_path

      fill_in 'user_first_name', with: 'Manny'
      fill_in 'user_last_name', with: 'York'
      fill_in 'user_email', with: 'manhattan@gmail.com'
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('User invited.')
    end

    scenario 'user accepts invitation and creates an account' do
      old_user = user
      invited_user = User.invite!(first_name: 'Brook', last_name: 'York', email: 'brooklyn@gmail.com',
                                  implementer: old_user.implementer, implementer_id: old_user.implementer_id)

      itoken = invited_user.raw_invitation_token

      visit "/users/invitation/accept?invitation_token=#{itoken}"

      expect(page.body).to have_content('Set your password')

      fill_in :user_password, with: 'Br00k1yn53^3R100!'
      fill_in :user_password_confirmation, with: 'Br00k1yn53^3R100!'
      check :user_agree_to_terms
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('Your password was set successfully. You are now signed in.')
      expect(page.body).to have_content("Welcome #{invited_user.name}")
    end
  end

  context 'when unsuccessful' do
    before(:each) do
      sign_in user
      visit new_user_invitation_path
    end

    scenario 'user does not fill out the invite form correctly' do
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('All fields are required to invite a new user.')

      fill_in 'user_first_name', with: 'Manny'
      fill_in 'user_last_name', with: 'York'
      fill_in 'user_email', with: 'manny@hogwarts.edu'
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('Email must be valid.')

      fill_in 'user_first_name', with: 'Manny'
      fill_in 'user_last_name', with: 'York'
      fill_in 'user_email', with: 'manhattan@gmail.com'
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('User invited.')
    end

    scenario 'user invites a user already signed up' do
      user1 = create(:user, email: 'email@gmail.com')

      fill_in 'user_first_name', with: user1.first_name
      fill_in 'user_last_name', with: user1.last_name
      fill_in 'user_email', with: user1.email
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('Email already exists in DPC.')
    end
  end
end
