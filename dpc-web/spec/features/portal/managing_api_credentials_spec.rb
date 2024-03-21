# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'managing api credentials' do
  include DpcClientSupport
  context 'as an unassigned user' do
    let!(:user) { create :user }

    before(:each) do
      sign_in user, scope: :user
    end

    it 'cannot manage api credentials' do
      visit portal_path
      expect(page).not_to have_css('[data-test="new-client-token"]')
      expect(page).not_to have_css('[data-test="new-public-key"]')
    end
  end

  context 'as a user assign to an org that is not credentialiable' do
    let!(:user) { create :user, :assigned }

    before(:each) do
      org = user.organizations.first
      org.update(npi: nil)

      sign_in user, scope: :user
    end

    it 'cannot manage api credentials' do
      visit portal_path
      expect(page).not_to have_css('[data-test="new-client-token"]')
      expect(page).not_to have_css('[data-test="new-public-key"]')
    end
  end

  context 'as an assigned user' do
    let!(:user) { create :user, :assigned }

    before(:each) do
      org = user.organizations.first
      org.update(npi: '3324567833')
      stub_api_client(
        message: :create_organization,
        success: true,
        response: default_org_creation_response
      )
      create(:registered_organization, organization: org, api_id: '923a4f7b-eade-494a-8ca4-7a685edacfad', enabled: true)

      sign_in user, scope: :user
    end

    scenario 'creating and viewing a client token' do
      api_client = stub_empty_key_request
      api_client = stub_empty_token_request(api_client)

      visit portal_path

      api_client = stub_token_creation_request(api_client)
      find('[data-test="new-client-token"]').click
      fill_in 'label', with: 'Sandbox Token 1'
      find('[data-test="form-submit"]').click

      expect(page).to have_content('Sandbox Token 1')
      expect(page).to have_content('1234567890')
      expect(page).to have_content('11/07/2019 at 5:15PM UTC')

      api_client = stub_key_get_request(api_client)
      stub_token_get_request(api_client)

      find('[data-test="portal-link"]').click

      expect(page).to have_content('Sandbox Token 1')
      expect(page).to have_content('11/07/2019 at 5:15PM UTC')
      expect(page).not_to have_content('1234567890')
    end

    scenario 'creating and viewing a public key' do
      api_client = stub_empty_key_request
      api_client = stub_empty_token_request(api_client)

      visit portal_path
      find('[data-test="new-public-key"]').click

      fill_in 'label', with: 'Sandbox Key 1'
      fill_in 'public_key', with: stubbed_key

      # FIXME this stubbing is a bit wonky
      api_client = stub_key_creation_request(api_client)
      api_client = stub_token_get_request(api_client)
      api_client = stub_key_get_request(api_client)

      find('[data-test="form-submit"]').click

      expect(page).to have_css('[data-test="new-public-key"]')
      expect(page).to have_content('3fa85f64-5717-4562-b3fc-2c963f66afa6')
    end
  end

  def stubbed_key
    file_fixture('stubbed_key.pem').read
  end

  def stub_key_creation_request(api_client=nil)
    stub_api_client(api_client:, message: :create_public_key, success: true, response: {
      'label' => 'Sandbox Key 1',
      'createdAt' => '2019-11-07T19:38:44.205Z',
      'id' => '3fa85f64-5717-4562-b3fc-2c963f66afa6'
    })
  end

  def stub_empty_key_request(api_client=nil)
    stub_api_client(api_client:, message: :get_public_keys, success: true, response: { 'entities' => [] })
  end

  def stub_empty_token_request(api_client=nil)
    stub_api_client(api_client:, message: :get_client_tokens, success: true, response: { 'entities' => [] })
  end

  def stub_key_get_request(api_client=nil)
    stub_api_client(api_client:, message: :get_public_keys, success: true, response: {
      'entities' => [{
        'id' => '3fa85f64-5717-4562-b3fc-2c963f66afa6',
        'publicKey' => '---PUBLIC KEY---......---END PUBLIC KEY---',
        'createdAt' => '2019-11-14T19:47:44.574Z',
        'label' => 'example public key'
      }],
      'count' => 1,
      'created_at' => '2019-11-14T19:47:44.574Z'
    })
  end

  def stub_token_creation_request(api_client=nil)
    stub_api_client(message: :create_client_token, success: true, response: {
      'token' => '1234567890',
      'label' => 'Sandbox Token 1',
      'createdAt' => '2019-11-07T17:15:22.781Z'
    })
  end

  def stub_token_get_request(api_client=nil)
    stub_api_client(api_client:, message: :get_client_tokens, success: true, response: {
      'entities' => [{
        'id' => '456a4f7b-ttwe-494a-8ca4-7a685edalrep',
        'tokenType' => 'MACAROON',
        'label' => 'Sandbox Token 1',
        'createdAt' => '2019-11-07T17:15:22.781Z',
        'expiresAt' => '2019-11-07T17:15:22.781Z'
      }]
    })
  end
end
