# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'user account expires' do
  context 'when successful' do
    scenario 'user account expires after 2 years of inactivity' do
      user = create(:user, password: '1!Abcdefghijklm', 
                    password_confirmation: '1!Abcdefghijklm', last_activity_at: 731.days.ago)

      visit new_user_session_path

      fill_in 'user_email', with: user.email
      fill_in 'user_password', with: '1!Abcdefghijklm'
      find('[data-test="submit"]').click

      expect(page).to have_content('Your account has expired due to inactivity. To reactivate your account, please contact us at dpcinfo@cms.hhs.gov.')
    end
  end
end
