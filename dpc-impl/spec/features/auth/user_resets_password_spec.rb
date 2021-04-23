# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user resets password' do
  let(:user) { create :user }

  context 'when successful' do
    scenario 'user resets password from recovery email' do
      visit new_user_password_path

      expect(page.body).to have_content('Forgot your password?')

      fill_in 'user_email', with: user.email
      find('input[data-test="submit"]').click
    end
  end

  # context 'when unsuccessful' do
  #   scenario 'user inputs invalid passwords before successful reset' do
  #   end

  #   scenario 'user inputs old password before successful reset' do
  #   end
  # end

  # context 'goes over email limit' do
  # end
end
