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

      sign_in user, scope: :user
    end

    scenario 'creating and viewing a client token' do
      stub_token_creation_request
      stub_token_get_request
      stub_key_get_request

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

    scenario 'creating and viewing a public key' do
      stub_key_creation_request
      stub_key_get_request
      stub_token_get_request
      visit dashboard_path
      find('[data-test="new-public-key"]').click
      select 'sandbox', from: 'api_environment'
      fill_in 'label', with: 'Sandbox Key 1'
      fill_in 'public_key', with: file_fixture("stubbed_cert.pem").read
      find('[data-test="form-submit"]').click

      # expect page location to be dashboard path
      expect(page).to have_content('3fa85f64-5717-4562-b3fc-2c963f66afa6')
    end
  end

  def stub_key_creation_request
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('MDAxY2xvY2F0aW9uIGh0dHA6Ly9teWJhbmsvCjAwMjZpZGVudGlmaWVyIHdlIHVzZWQgb3VyIHNlY3JldCBrZXkKMDAxNmNpZCB0ZXN0ID0gY2F2ZWF0CjAwMmZzaWduYXR1cmUgGXusegRK8zMyhluSZuJtSTvdZopmDkTYjOGpmMI9vWcK')
    stub_request(:post, 'http://dpc.example.com/Key/923a4f7b-eade-494a-8ca4-7a685edacfad').with(
      body: { label: 'Sandbox Key 1', key: file_fixture("stubbed_cert.pem").read }.to_json
    ).to_return(
      status: 200,
      body: { label: 'Sandbox Key 1', createdAt: '2019-11-07T19:38:44.205Z', id: '3fa85f64-5717-4562-b3fc-2c963f66afa6' }.to_json
    )
  end

  def stub_key_get_request
    stub_request(:get, 'http://dpc.example.com/Key/923a4f7b-eade-494a-8ca4-7a685edacfad')
      .to_return(
        status: 200,
        body: [
          {
            'id': '3fa85f64-5717-4562-b3fc-2c963f66afa6',
            'publicKey': {
              'algorithmId': {
                'algorithm': {
                  'id': 'string',
                  'encoded': [
                    'string'
                  ]
                },
                'parameters': {},
                'encoded': [
                  'string'
                ]
              },
              'algorithm': {
                'algorithm': {
                  'id': 'string',
                  'encoded': [
                    'string'
                  ]
                },
                'parameters': {},
                'encoded': [
                  'string'
                ]
              },
              'publicKeyData': {
                'padBits': 0,
                'string': 'string',
                'octets': [
                  'string'
                ],
                'loadedObject': {
                  'encoded': [
                    'string'
                  ]
                },
                'bytes': [
                  'string'
                ],
                'encoded': [
                  'string'
                ]
              },
              'publicKey': {
                'encoded': [
                  'string'
                ]
              },
              'encoded': [
                'string'
              ]
            },
            'createdAt': '2019-11-07T19:38:44.205Z'
          }
        ].to_json
      )
  end

  def stub_token_creation_request
    allow(ENV).to receive(:fetch).with('API_METADATA_URL_SANDBOX').and_return('http://dpc.example.com')
    allow(ENV).to receive(:fetch).with('GOLDEN_MACAROON_SANDBOX').and_return('MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciBiODY2NmVjMi1lOWY1LTRjODctYjI0My1jMDlhYjgyY2QwZTMKMDAyZnNpZ25hdHVyZSA1hzDOqfW_1hasj-tOps9XEBwMTQIW9ACQcZPuhAGxwwo')
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
