# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'admin signs in' do
  around do |example|
    OmniAuth.config.test_mode = true

    ClimateControl.modify GITHUB_ORG_TEAM_ID: '111222333' do
      example.run
    end

    OmniAuth.config.mock_auth[:github] = nil
  end

  context 'with github oauth' do
    context 'when user has valid github org and team' do
      before(:each) do
        github_client = double(Octokit::Client)
        allow(Octokit::Client).to receive(:new).and_return(github_client)
      end

      scenario 'creates new admin' do
        OmniAuth.config.mock_auth[:github] = OmniAuth::AuthHash.new(
          provider: 'github',
          uid: '123545',
          info: {
            nickname: 'whereisnemo',
            email: 'test@cms.hhs.gov',
            name: 'New Nemo',
            image: 'https://avatars3.githubusercontent.com/u/111'
          },
          credentials: { token: 'abcdefg' }
        )

        visit new_admin_session_path
        find('[data-test="admin-sign-in-form"]').click

        expect(page).to have_css('[data-test="admin-signout"]')
        expect(page).to have_content('test@cms.hhs.gov')
      end
    end
  end
end
