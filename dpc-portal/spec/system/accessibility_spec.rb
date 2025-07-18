# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :system do
  include Devise::Test::IntegrationHelpers
  include DpcClientSupport
  before do
    driven_by(:selenium_headless)
  end
  let(:dpc_api_organization_id) { 'some-gnarly-guid' }
  let(:axe_standard) { %w[best-practice wcag21aa] }
  context 'login' do
    it 'shows login page ok' do
      visit '/users/sign_in'
      expect(page).to_not have_text('You need to sign in or sign up before continuing.')
      expect(page).to have_text('Sign in')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'shows login failure' do
      visit '/users/auth/failure'
      expect(page).to have_text('Try again')
      expect(page).to be_axe_clean.according_to axe_standard
    end

    context 'bad user tries to log in' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:openid_connect,
                                 { uid: '12345',
                                   info: { email: 'bob@example.com' },
                                   extra: { raw_info: { all_emails: %w[bob@example.com bob2@example.com],
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
      end
      it 'shows no such user page' do
        visit '/users/auth/openid_connect/callback'
        expect(page).to have_text('You must have an account to sign in.')
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'shows sanctioned ao page' do
        create(:user, provider: :openid_connect, uid: '12345',
                      verification_status: 'rejected', verification_reason: 'ao_med_sanctions')
        visit '/users/auth/openid_connect/callback'
        expect(page).to have_text(I18n.t('verification.ao_med_sanctions_status'))
        expect(page).to be_axe_clean.according_to axe_standard
      end
    end
  end

  context 'organizations' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
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
      sign_in user
    end
    context 'list' do
      it 'empty' do
        visit '/organizations'
        expect(page).to have_text("You don't have any organizations to show.")
        expect(page).to be_axe_clean.according_to axe_standard
      end
      context 'ao' do
        before do
          org_good = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago, name: 'org_good')
          org_not_ao = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago, name: 'org_not_ao')
          org_bad_org = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago,
                                                       verification_status: :rejected,
                                                       verification_reason: :no_approved_enrollment,
                                                       name: 'org_bad_org')
          create(:ao_org_link, user:, provider_organization: org)
          create(:ao_org_link, user:, provider_organization: org_good)
          create(:ao_org_link, user:, provider_organization: org_not_ao,
                               verification_status: false,
                               verification_reason: :user_not_authorized_official)
          create(:ao_org_link, user:, provider_organization: org_bad_org)
        end
        it 'should show all organizations for ao' do
          visit '/organizations'
          expect(page).to_not have_text("You don't have any organizations to show.")
          expect(page).to have_text(I18n.t('verification.sign_tos'))
          expect(page).to have_text(I18n.t('verification.configuration_needed'))
          expect(page).to have_text(I18n.t('verification.access_denied'))
          expect(page).to be_axe_clean.according_to axe_standard
        end
      end
      context 'cd' do
        before do
          org_good = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago, name: 'org_good')
          create(:provider_organization, terms_of_service_accepted_at: 1.day.ago)
          org_bad_org = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago,
                                                       verification_status: :rejected,
                                                       verification_reason: :no_approved_enrollment,
                                                       name: 'org_bad_org')
          create(:cd_org_link, user:, provider_organization: org)
          create(:cd_org_link, user:, provider_organization: org_good)
          create(:cd_org_link, user:, provider_organization: org_bad_org)
        end
        it 'should show all organizations for cd' do
          visit '/organizations'
          expect(page).to_not have_text("You don't have any organizations to show.")
          expect(page).to have_text(I18n.t('cd_access.sign_tos'))
          expect(page).to have_text(I18n.t('cd_access.configuration_needed'))
          expect(page).to have_text(I18n.t('cd_access.access_denied'))
          expect(page).to be_axe_clean.according_to axe_standard
        end
      end
      context 'ao cd mix' do
        before do
          ao_org = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago, name: 'ao_org')
          cd_org = create(:provider_organization, terms_of_service_accepted_at: 1.day.ago, name: 'cd_org')
          create(:ao_org_link, user:, provider_organization: ao_org)
          create(:cd_org_link, user:, provider_organization: cd_org)
        end
        it 'should show all organizations for ao and cd' do
          visit '/organizations'
          expect(page).to_not have_text("You don't have any organizations to show.")
          expect(page).to have_text(I18n.t('verification.configuration_needed'))
          expect(page).to have_text(I18n.t('cd_access.configuration_needed'))
          expect(page).to be_axe_clean.according_to axe_standard
        end
      end
    end
    context 'page' do
      context :ao do
        let!(:ao_org_link) { create(:ao_org_link, user:, provider_organization: org) }
        it 'shows needs terms of service' do
          visit "/organizations/#{org.id}"
          expect(page).to have_text('Terms of Service')
          expect(page).to_not have_text('You can assign anyone as a CD')
          expect(page).to be_axe_clean.according_to axe_standard
        end
        it 'shows terms of service signed success' do
          visit "/organizations/#{org.id}"
          page.find('.usa-button', text: 'I have read and accepted the Terms of Service').click
          expect(page).to_not have_text('Terms of Service')
          expect(page).to have_text('You can assign anyone as a CD')
          expect(page).to be_axe_clean.according_to axe_standard
        end
        context 'tos signed' do
          before { org.update!(terms_of_service_accepted_by: user) }
          it 'should show with no cds' do
            visit "/organizations/#{org.id}"
            expect(page).to have_text('You can assign anyone as a CD')
            expect(page).to have_css('#credential_delegates')
            expect(page).to_not have_css('#credentials')
            expect(page).to_not have_css('#active-cd-table')
            expect(page).to_not have_css('#pending-cd-table')
            expect(page).to be_axe_clean.according_to axe_standard
          end
          it 'should show with no credentials' do
            visit "/organizations/#{org.id}"
            page.execute_script('make_current(1)')
            expect(page).to have_text('you must create a unique client token')
            expect(page).to_not have_css('#credential_delegates')
            expect(page).to have_css('#credentials')
            expect(page).to_not have_css('#client-tokens-table')
            expect(page).to_not have_css('#public-keys-table')
            expect(page).to_not have_css('#public-ips-table')
            expect(page).to be_axe_clean.according_to axe_standard
          end
          context 'with credential delegates' do
            let(:active_cd) { create(:user) }
            let!(:active_link) { create(:cd_org_link, user: active_cd, provider_organization: org) }
            let!(:invitation) { create(:invitation, :cd, provider_organization: org, invited_by: user) }
            it 'should show credential delegate tables' do
              visit "/organizations/#{org.id}"
              expect(page).to have_text('You can assign anyone as a CD')
              expect(page).to have_css('#active-cd-table')
              expect(page).to have_css('#pending-cd-table')
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
          context 'with tokens keys and ips' do
            let(:tokens) { default_get_client_tokens['entities'] }
            let(:keys) { default_get_public_keys['entities'] }
            let(:ip_addresses) { default_get_ip_addresses['entities'] }
            it 'should show credentials tables' do
              visit "/organizations/#{org.id}"
              page.execute_script('make_current(1)')
              expect(page).to have_css('#client-tokens-table')
              expect(page).to have_css('#public-keys-table')
              expect(page).to have_css('#public-ips-table')
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
          context 'create client token' do
            let(:tokens) { default_get_client_tokens['entities'] }
            it 'should show new page' do
              visit "/organizations/#{org.id}/client_tokens/new"
              expect(page).to have_text('Create a new client token')
              expect(page).to_not have_text('Label required')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show error page' do
              expect(mock_client_token_manager).to receive(:create_client_token).and_return({})
              expect(mock_client_token_manager).to receive(:errors).and_return({ root: 'Test Error' })
              visit "/organizations/#{org.id}/client_tokens/new"
              page.all('.usa-button')[1].click
              expect(page).to have_text('Test Error')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show success page' do
              success_response = { response: true, message: :token }
              expect(mock_client_token_manager).to receive(:create_client_token).and_return(success_response)
              visit "/organizations/#{org.id}/client_tokens/new"
              page.fill_in 'label', with: 'new token'
              page.all('.usa-button')[1].click
              expect(page).to have_text('Client token created')
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
          context 'create public key' do
            let(:keys) { default_get_public_keys['entities'] }
            it 'should show new page' do
              visit "/organizations/#{org.id}/public_keys/new"
              expect(page).to have_text('Add public key')
              expect(page).to_not have_text('Required values missing')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show error page' do
              error_response = { errors: { root: 'Test Error' } }
              expect(mock_public_key_manager).to receive(:create_public_key).and_return(error_response)
              visit "/organizations/#{org.id}/public_keys/new"
              page.all('.usa-button')[1].click
              expect(page).to have_text('Test Error')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show success page' do
              success_response = { response: { message: { 'id' => 'foo' } } }
              expect(mock_public_key_manager).to receive(:create_public_key).and_return(success_response)
              visit "/organizations/#{org.id}/public_keys/new"
              page.fill_in 'label', with: 'new key'
              page.fill_in 'public_key', with: 'key'
              page.all('.usa-button')[1].click
              expect(page).to have_text('Public key created successfully')
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
          context 'create IP address' do
            let(:keys) { default_get_ip_addresses['entities'] }
            it 'should show new page' do
              visit "/organizations/#{org.id}/ip_addresses/new"
              expect(page).to have_text('Add public IP address')
              expect(page).to_not have_text('Label required')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show error page' do
              error_response = { errors: { root: 'Test Error' } }
              expect(mock_ip_address_manager).to receive(:create_ip_address).and_return(error_response)
              visit "/organizations/#{org.id}/ip_addresses/new"
              page.find_button(value: 'Add IP').click
              expect(page).to have_text('Test Error')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show success page' do
              success_response = { response: { message: { 'id' => 'foo' } } }
              expect(mock_ip_address_manager).to receive(:create_ip_address).and_return(success_response)
              visit "/organizations/#{org.id}/ip_addresses/new"
              page.find_button(value: 'Add IP').click
              expect(page).to have_text('IP address created successfully')
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
          context 'create credential delegate invitation' do
            it 'should show new page' do
              visit "/organizations/#{org.id}/credential_delegate_invitations/new"
              expect(page).to have_text('Send invite')
              expect(page).to_not have_text("can't be blank")
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show error page' do
              visit "/organizations/#{org.id}/credential_delegate_invitations/new"
              page.find('.usa-button', text: 'Send invite').click
              page.find_button(value: 'Yes, I acknowledge').click
              expect(page).to have_text("can't be blank")
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show success page' do
              visit "/organizations/#{org.id}/credential_delegate_invitations/new"
              page.fill_in 'invited_given_name', with: 'John'
              page.fill_in 'invited_family_name', with: 'Lennon'
              page.fill_in 'invited_email', with: 'john@beatles.com'
              page.fill_in 'invited_email_confirmation', with: 'john@beatles.com'
              page.find('.usa-button', text: 'Send invite').click
              page.find_button(value: 'Yes, I acknowledge').click
              expect(page).to_not have_text("can't be blank")
              expect(page).to have_text('Credential Delegate invited successfully')
              expect(page).to be_axe_clean.according_to axe_standard
            end
            it 'should show duplicate error' do
              invitation = create(:invitation, :cd, provider_organization: org)
              visit "/organizations/#{org.id}/credential_delegate_invitations/new"
              page.fill_in 'invited_given_name', with: invitation.invited_given_name
              page.fill_in 'invited_family_name', with: invitation.invited_family_name
              page.fill_in 'invited_email', with: invitation.invited_email
              page.fill_in 'invited_email_confirmation', with: invitation.invited_email
              page.find('.usa-button', text: 'Send invite').click
              page.find_button(value: 'Yes, I acknowledge').click
              expect(page).to_not have_text("can't be blank")
              expect(page).to have_text(I18n.t('errors.attributes.base.duplicate_cd.status'))
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
        end
      end
      context :cd do
        let!(:cd_org_link) { create(:cd_org_link, user:, provider_organization: org) }
        it 'shows needs terms of service' do
          visit "/organizations/#{org.id}"
          expect(page).to have_text('is not ready for credential management')
          expect(page).to be_axe_clean.according_to axe_standard
        end
        context 'tos signed' do
          before { org.update!(terms_of_service_accepted_by: user) }
          it 'should show with no credentials' do
            visit "/organizations/#{org.id}"
            expect(page).to_not have_text('You can assign anyone as a CD')
            expect(page).to_not have_css('#credential_delegates')
            expect(page).to_not have_css('#client-tokens-table')
            expect(page).to_not have_css('#public-keys-table')
            expect(page).to_not have_css('#public-ips-table')
            expect(page).to have_text('you must create a unique client token')
            expect(page).to be_axe_clean.according_to axe_standard
          end
          context 'with tokens keys and ips' do
            let(:tokens) { default_get_client_tokens['entities'] }
            let(:keys) { default_get_public_keys['entities'] }
            let(:ip_addresses) { default_get_ip_addresses['entities'] }
            it 'should show credentials' do
              visit "/organizations/#{org.id}"
              expect(page).to have_css('#client-tokens-table')
              expect(page).to have_css('#public-keys-table')
              expect(page).to have_css('#public-ips-table')
              expect(page).to be_axe_clean.according_to axe_standard
            end
          end
        end
      end
    end
  end
  context 'ao invitation flow' do
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:invitation) { create(:invitation, :ao, provider_organization: org) }
    let!(:mock_uis) { instance_double(UserInfoService) }
    let(:user_info) do
      {
        'sub' => 'some-guid',
        'all_emails' => [invitation.invited_email],
        'email' => invitation.invited_email,
        'social_security_number' => '900111111'
      }
    end
    before do
      allow(UserInfoService).to receive(:new).and_return(mock_uis)
      allow(mock_uis).to receive(:user_info).and_return(user_info)
    end
    it 'should show intro page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}"
      expect(page).to have_text('Not be listed on the Medicare Exclusions Database')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show login page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
      expect(page).to have_text('Sign in or create')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show accept page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
      expect(page).to have_text('Step 3')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show register page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
      page.find('.usa-button', text: 'Continue to register').click
      expect(page).to have_text('Step 4')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show success page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
      page.find('.usa-button', text: 'Continue to register').click
      page.find('.usa-button', text: 'Complete registration').click
      expect(page).to have_text('Step 5')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    context :failure do
      let(:user_info) do
        {
          'sub' => 'some-guid',
          'all_emails' => [invitation.invited_email],
          'email' => invitation.invited_email,
          'social_security_number' => '900111112'
        }
      end
      let(:renew_success) { 'You should receive your new invitation shortly' }
      it 'should show bad invitation' do
        visit "/organizations/#{org.id}/invitations/bad-id"
        expect(page).to have_text('Your link is invalid.')
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'should show expired invitation' do
        invitation.update(created_at: 4.days.ago)
        visit "/organizations/#{org.id}/invitations/#{invitation.id}"
        expect(page).to have_text(I18n.t('verification.ao_expired_status'))
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'should show successful renewal of invitation' do
        invitation.update(created_at: 4.days.ago)
        visit "/organizations/#{org.id}/invitations/#{invitation.id}"
        page.find('.usa-button', text: 'Request new link').click
        expect(page).to have_text(renew_success)
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'should show already-renewed error' do
        invitation.update(status: :renewed)
        visit "/organizations/#{org.id}/invitations/#{invitation.id}"
        expect(page).to_not have_text(renew_success)
        expect(page).to have_text(I18n.t('verification.ao_renewed_text'))
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'should show email does not match error' do
        mismatched_invitation = create(:invitation, :ao, provider_organization: org,
                                                         invited_email: 'somethingelse@example.com',
                                                         invited_email_confirmation: 'somethingelse@example.com')
        visit "/organizations/#{org.id}/invitations/#{mismatched_invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{mismatched_invitation.id}/accept"
        expect(page).to have_text("The email you used to sign in doesn't match your invite.")
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'should show failed ao check' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        page.find('.usa-button', text: 'Continue to register').click
        expect(page).to have_text('Step 3')
        expect(page).to have_text('You’re not the Authorized Official.')
        expect(page).to be_axe_clean.according_to axe_standard
      end
    end
  end
  context :cd_invitation_flow do
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:invitation) { create(:invitation, :cd, provider_organization: org) }
    let!(:mock_uis) { instance_double(UserInfoService) }
    let(:user_info) do
      {
        'sub' => 'some-guid',
        'all_emails' => [invitation.invited_email],
        'email' => invitation.invited_email,
        'given_name' => invitation.invited_given_name,
        'family_name' => invitation.invited_family_name
      }
    end
    before do
      allow(UserInfoService).to receive(:new).and_return(mock_uis)
      allow(mock_uis).to receive(:user_info).and_return(user_info)
    end
    it 'should show intro page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}"
      expect(page).to have_text("You've been delegated to manage access")
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show login page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
      expect(page).to have_text('Sign in or create a Login.gov account')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show confirm page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
      expect(page).to have_text('Accept invite')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    it 'should show success page' do
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
      visit "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
      page.find('.usa-button', text: 'Accept invite').click
      expect(page).to have_text('Thank you for accepting your invite.')
      expect(page).to be_axe_clean.according_to axe_standard
    end
    context :failure do
      let(:user_info) do
        {
          'sub' => 'some-guid',
          'all_emails' => [invitation.invited_email],
          'email' => invitation.invited_email,
          'given_name' => 'Eustace',
          'family_name' => 'McGillicuddy'
        }
      end
      it 'should show expired invitation' do
        invitation.update(created_at: 4.days.ago)
        visit "/organizations/#{org.id}/invitations/#{invitation.id}"
        expect(page).to have_text(I18n.t('verification.cd_expired_status'))
        expect(page).to be_axe_clean.according_to axe_standard
      end
      it 'should show email does not match error' do
        mismatched_invitation = create(:invitation, :cd, provider_organization: org,
                                                         invited_email: 'somethingelse@example.com',
                                                         invited_email_confirmation: 'somethingelse@example.com')
        visit "/organizations/#{org.id}/invitations/#{mismatched_invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{mismatched_invitation.id}/confirm_cd"
        expect(page).to have_text(I18n.t('verification.pii_mismatch_status'))
        expect(page).to be_axe_clean.according_to axe_standard
      end
    end
  end
end
