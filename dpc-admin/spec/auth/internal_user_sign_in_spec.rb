# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'internal user signs in' do
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
        allow(github_client).to receive(:user_teams).and_return(
          [
            {
              name: 'dpc-test',
              id: '111222333',
              slug: 'dpc-test',
              privacy: 'closed',
              url: 'https://api.github.com/teams/111222333',
              organization: {
                login: 'CMSgov',
                id: '999888777',
                url: 'https://api.github.com/orgs/CMSgov'
              }
            }
          ]
        )
      end

      scenario 'creates new internal user' do
        # binding.pry
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

        visit new_internal_user_session_path
        find('[data-test="internal-user-sign-in-form"]').click

        expect(page).to have_css('[data-test="internal-user-signout"]')
        expect(page).to have_content('test@cms.hhs.gov')
      end
    end
  end
end
