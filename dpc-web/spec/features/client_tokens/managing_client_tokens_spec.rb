# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'managing client tokens' do
  context 'unassigned user' do
    let!(:user) { create :user }

    before(:each) do
      sign_in user, scope: :user
    end

    it 'cannot manage client tokens' do
      visit dashboard_path
      expect(page).not_to have_css('[data-test="new-client-token"]')
    end
  end

  context 'assigned user' do
    let!(:user) { create :user, :assigned }

    before(:each) do
      org = user.organizations.first
      org.update(api_environments: [0])
      create(:registered_organization, organization: org, api_env: 0, api_id: '923a4f7b-eade-494a-8ca4-7a685edacfad')

      stub_token_creation_request
      stub_token_get_request
      sign_in user, scope: :user
    end

    scenario 'creating and viewing a client token' do
      visit dashboard_path
      find('[data-test="new-client-token"]').click
      select 'sandbox', from: 'api_environment'
      fill_in 'label', with: 'Sandbox Token 1'
      find('[data-test="form-submit"]').click

      expect(page).to have_content('Sandbox Token 1')
      expect(page).to have_content('1234567890')
      expect(page).to have_content('11/07/2019 at 5:15PM UTC')

      find('[data-test="dashboard-link"]').click

      expect(page).to have_content('Sandbox Token 1')
      expect(page).to have_content('11/07/2019 at 5:15PM UTC')
      expect(page).not_to have_content('1234567890')
    end
  end

  def stub_token_creation_request
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('TURBeU0yeHZZMkYwYVc5dUlHaDBkSEE2THk5c2IyTmhiR2h2YzNRNk16QXdNZ293TURNMGFXUmxiblJwWm1sbGNpQmlOamczTURoak5TMDBOakV6TFRSbVlXVXRZVGhpWlMweU0ySXhNV1JoTVRFNVl6QUtNREF5Wm5OcFoyNWhkSFZ5WlNBcElYUUFpcmpYbE9NR3VaQkUyM25Ra2xYZjdmNlQ0RG5tc2RoeFk4cE9QQW8')
    stub_request(:post, 'http://dpc.example.com/Token').with(
      body: { label: 'Sandbox Token 1' }.to_json
    ).to_return(
      status: 200,
      body: { token: '1234567890', label: 'Sandbox Token 1', createdAt: '2019-11-07T17:15:22.781Z' }.to_json
    )
  end

  def stub_token_get_request
    stub_request(:get, "http://dpc.example.com/Token")
      .to_return(
        status: 200,
        body: {
          entities: [
            {
              id: '456a4f7b-ttwe-494a-8ca4-7a685edalrep',
              tokenType: 'MACAROON',
              label: 'Sandbox Token 1',
              createdAt: '2019-11-07T17:15:22.781Z',
              expiresAt: '2019-11-07T17:15:22.781Z'
            }
          ]
        }.to_json
      )
  end
end
