# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user can view invited users' do
  let (:user) { create :user }

  context 'can view invited users (invite pending and accepted)' do
    scenario 'user invites a new user and sees pending invite' do
      ashby = create(:user, first_name: 'Ashby', last_name: 'Santoso')

      sign_in ashby
      visit members_path

      fill_in 'user_first_name', with: 'Rosemary'
      fill_in 'user_last_name', with: 'Harper'
      fill_in 'user_email', with: 'harper@gmail.com'
      find('input[data-test="submit"]').click

      expect(page.body).to have_content('Rosemary Harper')
      expect(page.body).to have_content("(invited by #{ashby.name})")
    end

    scenario 'invited user accepts invitation and moves under Invited Users' do
      ashby = create(:user, first_name: 'Ashby', last_name: 'Santoso')
      rosemary = User.invite!(first_name: 'Rosemary', last_name: 'Harper',
                                  email: 'harper@gmail.com', implementer: ashby.implementer,
                                  implementer_id: ashby.implementer_id, invited_by_id: ashby.id)

      itoken = rosemary.raw_invitation_token

      visit "/users/invitation/accept?invitation_token=#{itoken}"

      expect(page.body).to have_content('Set your password')

      fill_in :user_password, with: '1!Abcdefghijklm'
      fill_in :user_password_confirmation, with: '1!Abcdefghijklm'
      check :user_agree_to_terms
      find('input[data-test="submit"]').click

      visit members_path

      expect(page.body).to have_content('Rosemary Harper')
      expect(page.body).not_to have_content("(invited by #{ashby.name})")
    end
  end
end
