# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :system do
  include Devise::Test::IntegrationHelpers
  include DpcClientSupport
  before do
    driven_by(:selenium_headless)
  end
  let(:api_id) { 'some-gnarly-guid' }
  let(:axe_standard) { %w[best-practice wcag21aa] }

  context 'login page' do
    it 'should be axe clean' do
      visit '/users/sign_in'
      expect(page).to have_text('Log in')
      expect(page).to be_axe_clean
    end

    it 'should be axe clean on failure' do
      visit '/users/sign_in'
      page.find('#login-button').click
      expect(page).to have_text('Invalid')
      expect(page).to be_axe_clean
    end
  end

  context 'registration page' do
    it 'should be axe clean' do
      visit '/users/sign_up'
      expect(page).to have_text('Request access')
      expect(page).to be_axe_clean
    end

    it 'should be axe clean on failure' do
      visit '/users/sign_up'
      expect(page).to have_text('Request access')
      find('#sign-up').click
      expect(page).to have_text("Email can't be blank")
      expect(page).to be_axe_clean
    end
  end

  context 'forgot password' do
    it 'should be axe clean' do
      visit '/users/password/new'
      expect(page).to have_text('Forgot')
      expect(page).to be_axe_clean
    end

    it 'should be axe clean on success' do
      visit '/users/password/new'
      expect(page).to have_text('Forgot')
      fill_in 'user_email', with: 'faker@fake.com'
      find('[data-test="submit"]').click
      expect(page).to have_text('If your email')
      expect(page).to be_axe_clean
    end
  end

  context 'resend confirmation' do
    it 'should be axe clean' do
      visit '/users/confirmation/new'
      expect(page).to have_text('Resend')
      expect(page).to be_axe_clean
    end

    it 'should be axe clean on success' do
      visit '/users/confirmation/new'
      expect(page).to have_text('Resend')
      fill_in 'user_email', with: 'faker@fake.com'
      find('[data-test="submit"]').click
      expect(page).to have_text('If your email')
      expect(page).to be_axe_clean
    end
  end

  context 'home page' do
    context 'unassigned user' do
      let!(:user) { create :user }
      it 'should be axe clean' do
        sign_in user, scope: :user
        visit authenticated_root_path
        expect(page).to have_text("We've received your access request.")
        expect(page).to be_axe_clean
      end
    end

    context 'assigned user' do
      let!(:user) { create :user }
      before(:each) do
        organization = create(:organization, organization_type: 'urgent_care', npi: '3324567833')
        stub_api_client(
          message: :create_organization,
          success: true,
          response: default_org_creation_response
        )
        create(:registered_organization, organization:, api_id:, enabled: true)
        user.organizations << organization
        sign_in user
      end

      it 'should be axe clean without credentials' do
        stub_multiple_call_client(messages: %i[
                                    get_public_keys
                                    get_client_tokens
                                  ],
                                  responses: [
                                    empty_response
                                  ])

        visit authenticated_root_path
        expect(page).to have_text('you must create a unique client token')
        expect(page).to have_text('add your public keys')
        expect(page).to be_axe_clean
      end

      it 'should be axe clean with credentials' do
        stub_multiple_call_client(messages: %i[
                                    get_public_keys
                                    get_client_tokens
                                  ],
                                  responses: [
                                    key_entities,
                                    token_entities
                                  ])
        visit authenticated_root_path
        expect(page).to_not have_text('you must create a unique client token')
        expect(page).to_not have_text('add your public keys')
        expect(page).to be_axe_clean
      end
    end
  end

  context 'credentials' do
    let!(:user) { create :user }

    before(:each) do
      organization = create(:organization, organization_type: 'urgent_care', npi: '3324567833')
      stub_api_client(
        message: :create_organization,
        success: true,
        response: default_org_creation_response
      )
      create(:registered_organization, organization:, api_id:, enabled: true)
      user.organizations << organization
      stub_multiple_call_client(messages: %i[
                                  get_public_keys
                                  get_client_tokens
                                  create_client_token
                                ],
                                responses: [
                                  empty_response,
                                  empty_response,
                                  empty_response,
                                  empty_response,
                                  create_token_response
                                ])
      sign_in user
    end

    context 'create token' do
      it 'should be axe clean' do
        visit authenticated_root_path
        find('[data-test="new-client-token"]').click
        expect(page).to have_text('New token for')
        expect(page).to be_axe_clean
      end

      it 'should be axe clean on failure' do
        visit authenticated_root_path
        find('[data-test="new-client-token"]').click
        expect(page).to have_text('New token for')
        find('[data-test="form-submit"]').click
        expect(page).to have_text('Label required')
        expect(page).to be_axe_clean
      end

      it 'should be axe clean on success' do
        visit authenticated_root_path
        find('[data-test="new-client-token"]').click
        expect(page).to have_text('New token for')
        fill_in 'label', with: 'Sandbox Token 1'
        find('[data-test="form-submit"]').click
        expect(page).to have_content('Success!')
        expect(page).to be_axe_clean
      end
    end

    context 'create public key' do
      it 'should be axe clean' do
        visit authenticated_root_path
        find('[data-test="new-public-key"]').click
        expect(page).to have_text('Upload Your Public Key')
        expect(page).to be_axe_clean
      end

      it 'should be axe clean on failure' do
        visit authenticated_root_path
        find('[data-test="new-public-key"]').click
        expect(page).to have_text('Upload Your Public Key')
        find('[data-test="form-submit"]').click
        expect(page).to have_text('Required values missing')
        expect(page).to be_axe_clean
      end
    end
  end

  context 'edit organization' do
    let!(:user) { create :user, :assigned }

    before(:each) do
      sign_in user, scope: :user
    end

    it 'should be axe clean' do
      visit authenticated_root_path
      find('[data-test="edit-link"]').click
      expect(page).to have_text('Edit:')
      expect(page).to be_axe_clean
    end

    it 'should be axe clean on failure' do
      visit authenticated_root_path
      find('[data-test="edit-link"]').click
      expect(page).to have_text('Edit:')
      fill_in 'organization_npi', with: :invalidnpi
      find('[data-test="form-submit"]').click
      expect(page).to have_text('Npi must be valid')
      expect(page).to be_axe_clean
    end
  end

  context 'edit user' do
    let!(:user) { create :user }

    before(:each) do
      sign_in user
    end

    it 'should be axe clean' do
      visit '/users/edit'
      expect(page).to have_text('Edit your info')
      expect(page).to be_axe_clean
    end

    it 'should be axe clean on failure' do
      visit '/users/edit'
      expect(page).to have_text('Edit your info')
      find('input[value="Update"]').click
      expect(page).to have_text("Current password can't be blank")
      expect(page).to be_axe_clean
    end
  end
end

def stubbed_key
  file_fixture('stubbed_key.pem').read
end

def empty_response
  { 'entities' => [] }
end

def create_token_response
  {
    'token' => '1234567890',
    'label' => 'Sandbox Token 1',
    'createdAt' => '2019-11-07T17:15:22.781Z'
  }
end

def token_entities
  {
    'entities' => [{
      'id' => 'client-token-guid',
      'tokenType' => 'MACAROON',
      'label' => 'Sandbox Token 1',
      'createdAt' => '2019-11-07T17:15:22.781Z',
      'expiresAt' => '2019-11-07T17:15:22.781Z'
    }]
  }
end

def key_entities
  {
    'entities' => [{
      'id' => '3fa85f64-5717-4562-b3fc-2c963f66afa6',
      'publicKey' => '---PUBLIC KEY---......---END PUBLIC KEY---',
      'createdAt' => '2019-11-14T19:47:44.574Z',
      'label' => 'example public key'
    }],
    'count' => 1,
    'created_at' => '2019-11-14T19:47:44.574Z'
  }
end
