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
      sign_in user, scope: :user
    end

    scenario 'creating a client token and downloading it' do
      # start up fake FS

      api_client = instance_double(APIClient)
      allow(APIClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:create_client_token).and_return({ token: '67890qq' })

      visit dashboard_path
      find('[data-test="new-client-token"]').click
      fill_in 'description', with: 'Sandbox Token 1'
      find('[data-test="form-submit"]').click
      expect(page).to have_content('Sandbox Token 1')
      expect(page).to have_content(token)

      # click download button
      # expect(File.read(downloaded_file).to have_content(token)
    end

    scenario 'deleting a client token' do
      api_client = instance_double(APIClient)
      allow(APIClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:get_client_tokens).and_return([{ id: '1', description: 'Sandbox Token 1' }])

      expect(page).to have_content('Sandbox Token 1')
      find('[data-test="delete-token-1"]').click

      expect(page).not_to have_content('Sandbox Token 1')
    end
  end
end
