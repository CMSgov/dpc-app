# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::CredentialDelegate::NewInvitationComponent, type: :system, js: true do
  include DpcClientSupport

  before do
    driven_by(:selenium_headless)
  end
  let(:uid) { '12345' }

  before do
    OmniAuth.config.test_mode = true
    # OmniAuth.config.add_mock(:id_me,
    OmniAuth.config.add_mock(:clear,
                             { uid:,
                               info: { email: 'bob@example.com' },
                               extra: { raw_info: { all_emails: %w[bob@example.com bob2@example.com],
                                                    ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
  end
  def sign_in
    # visit '/auth/id_me/callback'
    visit '/auth/clear/callback'
  end
  context 'CD invite' do
    let(:dpc_api_organization_id) { 'some-gnarly-guid' }
    # let!(:user) { create(:user, provider: :id_me, uid: '12345') }
    let!(:user) { create(:user, provider: :clear, uid: '12345') }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:ao_org_link) { create(:ao_org_link, user:, provider_organization: org) }

    before do
      sign_in
      org.update!(terms_of_service_accepted_by: user)
    end

    context 'Successful invitation' do
      let(:mock_client_token_manager) { instance_double(ClientTokenManager) }
      let(:mock_public_key_manager) { instance_double(PublicKeyManager) }
      let(:mock_ip_address_manager) { instance_double(IpAddressManager) }
      let(:tokens) { [] }
      let(:keys) { [] }
      let(:ip_addresses) { [] }

      before do
        allow(ClientTokenManager).to receive(:new).and_return(mock_client_token_manager)
        allow(PublicKeyManager).to receive(:new).and_return(mock_public_key_manager)
        allow(IpAddressManager).to receive(:new).and_return(mock_ip_address_manager)
        allow(mock_client_token_manager).to receive(:client_tokens).and_return(tokens)
        allow(mock_public_key_manager).to receive(:public_keys).and_return(keys)
        allow(mock_ip_address_manager).to receive(:ip_addresses).and_return(ip_addresses)
      end

      it 'can invite cd' do
        visit "/organizations/#{org.id}/credential_delegate_invitations/new"
        expect(page).to have_selector('#verify-modal', visible: false)

        # Fill in the form and click send invite
        fill_in 'First or given name', with: 'John'
        fill_in 'Last or family name', with: 'Smith'
        fill_in 'Email', with: 'fake@fake.com'
        fill_in 'Confirm email', with: 'fake@fake.com'
        within('#cd-form') do
          click_button('Send invite')
        end
        expect(page).to have_selector('#verify-modal', visible: true)

        click_button('Yes, I acknowledge')

        # Make sure the invitation was created with the correct info
        invitation = Invitation.last
        expect(invitation.provider_organization_id).to eq(org.id)
        expect(invitation.invited_given_name).to eq('John')
        expect(invitation.invited_family_name).to eq('Smith')
        expect(invitation.invited_email).to eq('fake@fake.com')
      end
    end

    it 'shows error on blank given name' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'First or given name', with: ''
      find_field('First or given name').send_keys(:tab)
      expect(page).to have_css('p#invited_given_name_error_msg', text: "Can't be blank")
    end

    it 'shows error on blank family name' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'Last or family name', with: ''
      find_field('Last or family name').send_keys(:tab)
      expect(page).to have_css('p#invited_family_name_error_msg', text: "Can't be blank")
    end

    it 'shows error on blank email' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'Email', with: ''
      find_field('Email').send_keys(:tab)
      expect(page).to have_css('p#invited_email_error_msg', text: "Can't be blank")
    end

    it 'shows error on invalid email' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'Email', with: 'not-an-email'
      find_field('Email').send_keys(:tab)
      expect(page).to have_css('p#invited_email_error_msg', text: 'Invalid email format')
    end

    it 'shows error on blank email confirmation' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'Confirm email', with: ''
      find_field('Confirm email').send_keys(:tab)
      expect(page).to have_css('p#invited_email_confirmation_error_msg', text: "Can't be blank")
    end

    it 'shows error on email confirmation mismatch' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'Email', with: 'fake@fake.com'
      fill_in 'Confirm email', with: 'notfake@fake.com'
      find_field('Confirm email').send_keys(:tab)
      expect(page).to have_css('p#invited_email_confirmation_error_msg', text: "Email doesn't match")
    end

    it 'shows error on invalid email confirmation' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      fill_in 'Email', with: 'not-an-email'
      fill_in 'Confirm email', with: 'not-an-email'
      find_field('Confirm email').send_keys(:tab)
      expect(page).to have_css('p#invited_email_confirmation_error_msg', text: 'Invalid email format')
    end
  end
end
